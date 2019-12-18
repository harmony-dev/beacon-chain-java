package org.ethereum.beacon.chain;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.HeadFunction;
import org.ethereum.beacon.consensus.spec.ForkChoice.LatestMessage;
import org.ethereum.beacon.consensus.spec.ForkChoice.Store;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * The beacon chain fork choice rule is a hybrid that combines justification and finality with
 * Latest Message Driven (LMD) Greediest Heaviest Observed SubTree (GHOST). For more info check <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#beacon-chain-fork-choice-rule">Beacon
 * chain fork choice rule</a>
 */
public class LMDGhostHeadFunction implements HeadFunction {

  private final BeaconChainStorage chainStorage;
  private final BeaconChainSpec spec;
  private final int SEARCH_LIMIT = Integer.MAX_VALUE;

  public LMDGhostHeadFunction(BeaconChainStorage chainStorage, BeaconChainSpec spec) {
    this.chainStorage = chainStorage;
    this.spec = spec;
  }

  @Override
  public BeaconBlock getHead(
      Function<ValidatorIndex, Optional<LatestMessage>> latestAttestationStorage) {
    Hash32 justifiedRoot =
        chainStorage
            .getJustifiedStorage()
            .get()
            .orElseThrow(() -> new RuntimeException("Justified root is not found"))
            .getRoot();
    Optional<BeaconTuple> justifiedTuple = chainStorage
        .getTupleStorage()
        .get(justifiedRoot);
    if (!justifiedTuple.isPresent()) {
      throw new RuntimeException("Justified block is not found");
    }

    Hash32 headRoot = spec.get_head(new Store() {

      @Override
      public Time getTime() {
        throw new UnsupportedOperationException("not yet implemented");
      }

      @Override
      public void setTime(Time time) {
        throw new UnsupportedOperationException("not yet implemented");
      }

      @Override
      public Time getGenesisTime() {
        throw new UnsupportedOperationException("not yet implemented");
      }

      @Override
      public void setGenesisTime(Time time) {
        throw new UnsupportedOperationException("not yet implemented");
      }

      @Override
      public Checkpoint getJustifiedCheckpoint() {
        return chainStorage.getJustifiedStorage().get().get();
      }

      @Override
      public void setJustifiedCheckpoint(Checkpoint checkpoint) {
        chainStorage.getBestJustifiedStorage().set(checkpoint);
      }

      @Override
      public Checkpoint getBestJustifiedCheckpoint() {
        return chainStorage.getBestJustifiedStorage().get().get();
      }

      @Override
      public void setBestJustifiedCheckpoint(Checkpoint checkpoint) {
        chainStorage.getBestJustifiedStorage().set(checkpoint);
      }

      @Override
      public Checkpoint getFinalizedCheckpoint() {
        return chainStorage.getFinalizedStorage().get().get();
      }

      @Override
      public void setFinalizedCheckpoint(Checkpoint checkpoint) {
        chainStorage.getFinalizedStorage().set(checkpoint);
      }

      @Override
      public Optional<BeaconState> getCheckpointState(Checkpoint checkpoint) {
        throw new UnsupportedOperationException("not yet implemented");
      }

      @Override
      public void setCheckpointState(Checkpoint checkpoint, BeaconState state) {
        throw new UnsupportedOperationException("not yet implemented");
      }

      @Override
      public Optional<BeaconBlock> getBlock(Hash32 root) {
        return chainStorage.getBlockStorage().get(root);
      }

      @Override
      public void setBlock(Hash32 root, BeaconBlock block) {
        throw new UnsupportedOperationException("not yet implemented");
      }

      @Override
      public Optional<BeaconState> getState(Hash32 root) {
        return chainStorage.getStateStorage().get(root);
      }

      @Override
      public void setState(Hash32 root, BeaconState state) {
        throw new UnsupportedOperationException("not yet implemented");
      }

      @Override
      public Optional<LatestMessage> getLatestMessage(ValidatorIndex index) {
        return latestAttestationStorage.apply(index);
      }

      @Override
      public void setLatestMessage(ValidatorIndex index, LatestMessage message) {
        throw new UnsupportedOperationException("not yet implemented");
      }

      @Override
      public List<Hash32> getChildren(Hash32 root) {
        return chainStorage.getBlockStorage().getChildren(root, SEARCH_LIMIT).stream()
            .map(spec::signing_root).collect(Collectors.toList());
      }
    });

    // Forcing get() call is save here as
    // it's been checked above that this particular root matches to a block
    return chainStorage.getBlockStorage().get(headRoot).get();
  }

  /**
   * Let get_latest_attestation(store, validator) be the attestation with the highest slot number in
   * store from validator. If several such attestations exist, use the one the validator v observed
   * first.
   */
  private Optional<Attestation> get_latest_attestation(
      Function<ValidatorRecord, Optional<Attestation>> latestAttestationStorage,
      ValidatorRecord validatorRecord) {
    return latestAttestationStorage.apply(validatorRecord);
  }
}
