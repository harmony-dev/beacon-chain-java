package org.ethereum.beacon.chain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.spec.ForkChoice.LatestMessage;
import org.ethereum.beacon.consensus.spec.ForkChoice.Store;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class LMDGhostProcessor implements ForkChoiceProcessor {

  private final int SEARCH_LIMIT = Integer.MAX_VALUE;

  private final BeaconChainSpec spec;
  private final BeaconChainStorage storage;
  private final SimpleProcessor<BeaconChainHead> chainHeadStream;

  private final Map<ValidatorIndex, LatestMessage> latestMessageStorage = new HashMap<>();
  private Checkpoint justifiedCheckpoint = Checkpoint.EMPTY;
  private Hash32 currentHeadRoot = Hash32.ZERO;

  public LMDGhostProcessor(
      BeaconChainSpec spec,
      BeaconChainStorage storage,
      Schedulers schedulers,
      Publisher<Checkpoint> justifiedCheckpoints,
      Publisher<IndexedAttestation> wireAttestations,
      Publisher<? extends BeaconTuple> importedBlocks) {
    this.spec = spec;
    this.storage = storage;

    Scheduler scheduler = schedulers.newSingleThreadDaemon("lmd-ghost-processor").toReactor();
    this.chainHeadStream = new SimpleProcessor<>(scheduler, "LMDGhostProcessor.chainHeadStream");

    Flux.from(justifiedCheckpoints).publishOn(scheduler).subscribe(this::onNewJustifiedCheckpoint);
    Flux.from(wireAttestations).publishOn(scheduler).subscribe(this::onNewAttestation);
    Flux.from(importedBlocks).publishOn(scheduler).subscribe(this::onNewImportedBlock);
  }

  private void onNewImportedBlock(BeaconTuple tuple) {
    if (!isJustifiedAncestor(tuple.getBlock())) {
      return;
    }

    for (Attestation attestation : tuple.getBlock().getBody().getAttestations()) {
      List<ValidatorIndex> indices =
          spec.get_attesting_indices(
              tuple.getState(), attestation.getData(), attestation.getAggregationBits());
      processAttestation(indices, attestation.getData());
    }

    updateHead();
  }

  private boolean isJustifiedAncestor(BeaconBlock block) {
    // genesis shortcut
    if (justifiedCheckpoint.equals(Checkpoint.EMPTY) && block.getSlot().equals(SlotNumber.ZERO)) {
      return true;
    }

    BeaconBlock ancestor = block;
    while (spec.compute_epoch_of_slot(ancestor.getSlot())
        .greaterEqual(justifiedCheckpoint.getEpoch())) {
      Optional<BeaconBlock> parent = storage.getBlockStorage().get(ancestor.getParentRoot());
      if (!parent.isPresent()) {
        return false;
      }
      if (parent.get().getParentRoot().equals(justifiedCheckpoint.getRoot())) {
        return true;
      }
      ancestor = parent.get();
    }

    return false;
  }

  private void onNewAttestation(IndexedAttestation attestation) {
    List<ValidatorIndex> indices = new ArrayList<>(attestation.getCustodyBit0Indices().listCopy());
    indices.addAll(attestation.getCustodyBit1Indices().listCopy());
    processAttestation(indices, attestation.getData());
    updateHead();
  }

  private void processAttestation(List<ValidatorIndex> indices, AttestationData data) {
    LatestMessage message =
        new LatestMessage(data.getTarget().getEpoch(), data.getBeaconBlockRoot());
    indices.forEach(
        index -> {
          latestMessageStorage.merge(
              index,
              message,
              (oldMessage, newMessage) -> {
                if (newMessage.getEpoch().greater(oldMessage.getEpoch())) {
                  return newMessage;
                } else {
                  return oldMessage;
                }
              });
        });
  }

  private void updateHead() {
    Hash32 newHeadRoot = getHeadRoot();
    if (!newHeadRoot.equals(currentHeadRoot)) {
      BeaconTuple tuple = storage.getTupleStorage().get(newHeadRoot).get();
      currentHeadRoot = newHeadRoot;
      chainHeadStream.onNext(new BeaconChainHead(tuple));
    }
  }

  private Hash32 getHeadRoot() {
    return spec.get_head(
        new Store() {

          @Override
          public Checkpoint getJustifiedCheckpoint() {
            return storage.getJustifiedStorage().get().get();
          }

          @Override
          public Checkpoint getFinalizedCheckpoint() {
            return storage.getFinalizedStorage().get().get();
          }

          @Override
          public Optional<BeaconBlock> getBlock(Hash32 root) {
            return storage.getBlockStorage().get(root);
          }

          @Override
          public Optional<BeaconState> getState(Hash32 root) {
            return storage.getStateStorage().get(root);
          }

          @Override
          public Optional<LatestMessage> getLatestMessage(ValidatorIndex index) {
            return Optional.ofNullable(latestMessageStorage.get(index));
          }

          @Override
          public List<Hash32> getChildren(Hash32 root) {
            return storage.getBlockStorage().getChildren(root, SEARCH_LIMIT).stream()
                .map(spec::signing_root)
                .collect(Collectors.toList());
          }
        });
  }

  private void onNewJustifiedCheckpoint(Checkpoint checkpoint) {
    if (checkpoint.getEpoch().greater(justifiedCheckpoint.getEpoch())) {
      justifiedCheckpoint = checkpoint;
      resetLatestMessages();
      updateHead();
    }
  }

  private void resetLatestMessages() {
    latestMessageStorage.clear();
  }

  @Override
  public Publisher<BeaconChainHead> getChainHeads() {
    return chainHeadStream;
  }
}
