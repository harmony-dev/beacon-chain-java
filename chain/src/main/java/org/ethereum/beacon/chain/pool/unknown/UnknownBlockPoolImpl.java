package org.ethereum.beacon.chain.pool.unknown;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import org.ethereum.beacon.chain.pool.AbstractProcessor;
import org.ethereum.beacon.chain.pool.AttestationPool;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.UnknownBlockPool;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.RunnableEx;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class UnknownBlockPoolImpl extends AbstractProcessor implements UnknownBlockPool {

  /** prev + curr + lookahead */
  private static final EpochNumber TRACKED_EPOCHS =
      EpochNumber.of(2).plus(AttestationPool.MAX_ATTESTATION_LOOKAHEAD);

  private final Queue queue = new Queue(TRACKED_EPOCHS, AttestationPool.UNKNOWN_BLOCK_POOL_SIZE);

  private final SimpleProcessor<ReceivedAttestation> unknownBlock;
  private final Scheduler executor;
  private final BeaconBlockStorage blockStorage;
  private final BeaconChainSpec spec;

  private EpochNumber currentBaseLine;

  public UnknownBlockPoolImpl(
      Schedulers schedulers,
      Publisher<SlotNumber> slotClock,
      Publisher<BeaconBlock> importedBlocks,
      Scheduler executor,
      BeaconBlockStorage blockStorage,
      BeaconChainSpec spec) {
    super(schedulers, "UnknownBlockPool");

    this.blockStorage = blockStorage;
    this.spec = spec;
    this.executor = executor;

    Flux.from(slotClock).subscribe(this::onNewSlot);
    Flux.from(importedBlocks).subscribe(this::onNewImportedBlock);
    this.unknownBlock =
        new SimpleProcessor<>(schedulers.events(), "UnknownChainPool.unknownChainAttestations");
  }

  @Override
  public Publisher<ReceivedAttestation> out() {
    return outbound;
  }

  @Override
  public void in(ReceivedAttestation attestation) {
    if (isInitialized()) {
      execute(() -> processAttestation(attestation));
    }
  }

  private void processAttestation(ReceivedAttestation attestation) {
    AttestationData data = attestation.getMessage().getData();

    // beacon block has not yet been imported
    // it implies that source and target blocks might have not been imported too
    if (blockStorage.get(data.getBeaconBlockRoot()).isPresent()) {
      outbound.onNext(attestation);
    } else {
      unknownBlock.onNext(attestation);
      queue.add(data.getTarget().getEpoch(), data.getBeaconBlockRoot(), attestation);
    }
  }

  @VisibleForTesting
  void onNewImportedBlock(BeaconBlock block) {
    EpochNumber blockEpoch = spec.compute_epoch_of_slot(block.getSlot());
    // blockEpoch < currentBaseLine || blockEpoch >= currentBaseLine + TRACKED_EPOCHS
    if (blockEpoch.less(currentBaseLine)
        || blockEpoch.greaterEqual(currentBaseLine.plus(TRACKED_EPOCHS))) {
      return;
    }

    execute(
        () -> {
          Hash32 blockRoot = spec.hash_tree_root(block);
          List<ReceivedAttestation> attestations = queue.evict(blockRoot);
          attestations.forEach(outbound::onNext);
        });
  }

  @VisibleForTesting
  void onNewSlot(SlotNumber slotNumber) {
    EpochNumber currentEpoch = spec.compute_epoch_of_slot(slotNumber);
    EpochNumber baseLine =
        currentEpoch.equals(spec.getConstants().getGenesisEpoch())
            ? currentEpoch
            : currentEpoch.decrement();
    queue.moveBaseLine(baseLine);

    this.currentBaseLine = baseLine;
  }

  @VisibleForTesting
  boolean isInitialized() {
    return currentBaseLine != null;
  }

  private void execute(RunnableEx routine) {
    executor.execute(routine);
  }

  @Override
  public Publisher<ReceivedAttestation> unknownBlock() {
    return unknownBlock;
  }
}
