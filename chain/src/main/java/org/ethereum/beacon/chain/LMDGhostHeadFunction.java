package org.ethereum.beacon.chain;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.HeadFunction;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
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
      Function<ValidatorRecord, Optional<Attestation>> latestAttestationStorage) {
    Hash32 justifiedRoot =
        chainStorage
            .getJustifiedStorage()
            .get()
            .orElseThrow(() -> new RuntimeException("Justified root is not found"));
    BeaconTuple justifiedTuple =
        chainStorage
            .getTupleStorage()
            .get(justifiedRoot)
            .orElseThrow(() -> new RuntimeException("Justified block is not found"));

    Function<Hash32, List<BeaconBlock>> getChildrenBlocks =
        (hash) -> chainStorage.getBlockStorage().getChildren(hash, SEARCH_LIMIT);
    BeaconBlock newHead =
        spec.lmd_ghost(
            justifiedTuple.getBlock(),
            justifiedTuple.getState(),
            chainStorage.getBlockStorage()::get,
            getChildrenBlocks,
            validatorRecord -> get_latest_attestation(latestAttestationStorage, validatorRecord));

    return newHead;
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
