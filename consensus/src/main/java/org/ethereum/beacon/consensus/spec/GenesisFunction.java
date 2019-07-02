package org.ethereum.beacon.consensus.spec;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.List;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * On genesis part.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/core/0_beacon-chain.md#genesis">Genesis</a>
 *     in the spec.
 */
public interface GenesisFunction extends BlockProcessing {

  /*
   """
   Get an empty ``BeaconBlock``.
   """
  */
  default BeaconBlock get_empty_block() {
    BeaconBlockBody body =
        BeaconBlockBody.create(
            BLSSignature.ZERO,
            new Eth1Data(Hash32.ZERO, UInt64.ZERO, Hash32.ZERO),
            Bytes32.ZERO,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList());
    return new BeaconBlock(
        getConstants().getGenesisSlot(), Hash32.ZERO, Hash32.ZERO, body, BLSSignature.ZERO);
  }

  /*
   """
   Get the genesis ``BeaconState``.
   """
  */
  default BeaconState get_genesis_beacon_state(
      List<Deposit> genesisValidatorDeposits, Time genesisTime, Eth1Data genesisEth1Data) {
    MutableBeaconState state = BeaconState.getEmpty(getConstants()).createMutableCopy();

    // Misc
    state.setSlot(getConstants().getGenesisSlot());
    state.setGenesisTime(genesisTime);
    state.setFork(new Fork(
        int_to_bytes4(UInt64.ZERO),
        int_to_bytes4(UInt64.ZERO),
        getConstants().getGenesisEpoch()));

    // Validator registry
    state.getValidators().clear();
    state.getBalances().clear();

    // Randomness and committees
    state.getRandaoMixes().setAll(Hash32.ZERO);
    state.setStartShard(ShardNumber.ZERO);

    // Finality
    state.getPreviousEpochAttestations().clear();
    state.getCurrentEpochAttestations().clear();
    state.setPreviousJustifiedCheckpoint(
        new Checkpoint(getConstants().getGenesisEpoch(), Hash32.ZERO));
    state.setCurrentJustifiedCheckpoint(
        new Checkpoint(getConstants().getGenesisEpoch(), Hash32.ZERO));
    state.setJustificationBits(Bitfield64.ZERO);
    state.setFinalizedCheckpoint(new Checkpoint(getConstants().getGenesisEpoch(), Hash32.ZERO));

    // Recent state
    state.getPreviousCrosslinks().setAll(Crosslink.EMPTY);
    state.getCurrentCrosslinks().setAll(Crosslink.EMPTY);
    state.getBlockRoots().setAll(Hash32.ZERO);
    state.getStateRoots().setAll(Hash32.ZERO);
    state.getActiveIndexRoots().setAll(Hash32.ZERO);
    state.getSlashings().setAll(Gwei.ZERO);
    state.setLatestBlockHeader(get_temporary_block_header(get_empty_block()));
    state.getHistoricalRoots().clear();

    // Ethereum 1.0 chain data
    state.setEth1Data(genesisEth1Data);
    state.getEth1DataVotes().clear();
    state.setEth1DepositIndex(UInt64.ZERO);

    // Process genesis deposits
    for (Deposit deposit : genesisValidatorDeposits) {
      process_deposit(state, deposit);
    }

    // Process genesis activations
    for (ValidatorIndex validatorIndex : state.getValidators().size().iterateFromZero()) {
      ValidatorRecord validator = state.getValidators().get(validatorIndex);
      if (validator.getEffectiveBalance().greaterEqual(getConstants().getMaxEffectiveBalance())) {
        state.getValidators().update(validatorIndex,
            record -> ValidatorRecord.Builder.fromRecord(record)
                .withActivationEpoch(getConstants().getGenesisEpoch())
                .withActivationEligibilityEpoch(getConstants().getGenesisEpoch()).build());
      }
    }

    Hash32 genesisActiveIndexRoot = hash_tree_root(
        get_active_validator_indices(state, getConstants().getGenesisEpoch()));

    for (EpochNumber index : getConstants().getEpochsPerHistoricalVector().iterateFrom(EpochNumber.ZERO)) {
      state.getActiveIndexRoots().set(index, genesisActiveIndexRoot);
    }

    return state.createImmutable();
  }

  default boolean is_genesis_trigger(List<Deposit> deposits, Eth1Data genesisEth1Data, UInt64 timestamp) {
    // Process deposits
    MutableBeaconState state = BeaconState.getEmpty(getConstants()).createMutableCopy();
    state.setEth1Data(genesisEth1Data);
    deposits.forEach(d -> {
      verify_deposit(state, d);
      process_deposit(state, d);
    });

    // Count active validators at genesis
    int active_validator_count = 0;
    for (ValidatorRecord validator : state.getValidators()) {
      if (validator.getEffectiveBalance().equals(getConstants().getMaxEffectiveBalance())) {
        active_validator_count += 1;
      }
    }

    // Check effective balance to trigger genesis
    return active_validator_count == getConstants().getGenesisActiveValidatorCount();
  }
}
