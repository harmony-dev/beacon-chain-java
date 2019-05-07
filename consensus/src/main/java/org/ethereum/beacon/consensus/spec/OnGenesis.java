package org.ethereum.beacon.consensus.spec;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;

import java.util.List;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
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
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.1/specs/core/0_beacon-chain.md#on-genesis">On
 *     genesis</a> in the spec.
 */
public interface OnGenesis extends BlockProcessing {

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
    state.getValidatorRegistry().clear();
    state.getBalances().clear();

    // Randomness and committees
    state.getLatestRandaoMixes().setAll(Hash32.ZERO);
    state.setLatestStartShard(ShardNumber.ZERO);

    // Finality
    state.getPreviousEpochAttestations().clear();
    state.getCurrentEpochAttestations().clear();
    state.setPreviousJustifiedEpoch(getConstants().getGenesisEpoch());
    state.setCurrentJustifiedEpoch(getConstants().getGenesisEpoch());
    state.setPreviousJustifiedRoot(Hash32.ZERO);
    state.setCurrentJustifiedRoot(Hash32.ZERO);
    state.setJustificationBitfield(Bitfield64.ZERO);
    state.setFinalizedEpoch(getConstants().getGenesisEpoch());
    state.setFinalizedRoot(Hash32.ZERO);

    // Recent state
    state.getPreviousCrosslinks().setAll(Crosslink.EMPTY);
    state.getCurrentCrosslinks().setAll(Crosslink.EMPTY);
    state.getLatestBlockRoots().setAll(Hash32.ZERO);
    state.getLatestStateRoots().setAll(Hash32.ZERO);
    state.getLatestActiveIndexRoots().setAll(Hash32.ZERO);
    state.getLatestSlashedBalances().setAll(Gwei.ZERO);
    state.setLatestBlockHeader(get_temporary_block_header(get_empty_block()));
    state.getHistoricalRoots().clear();

    // Ethereum 1.0 chain data
    state.setLatestEth1Data(genesisEth1Data);
    state.getEth1DataVotes().clear();
    state.setDepositIndex(UInt64.ZERO);

    // Process genesis deposits
    for (Deposit deposit : genesisValidatorDeposits) {
      process_deposit(state, deposit);
    }

    // Process genesis activations
    for (ValidatorIndex validatorIndex : state.getValidatorRegistry().size().iterateFromZero()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(validatorIndex);
      if (validator.getEffectiveBalance().greaterEqual(getConstants().getMaxEffectiveBalance())) {
        state.getValidatorRegistry().update(validatorIndex,
            record -> ValidatorRecord.Builder.fromRecord(record)
                .withActivationEpoch(getConstants().getGenesisEpoch())
                .withActivationEligibilityEpoch(getConstants().getGenesisEpoch()).build());
      }
    }

    Hash32 genesisActiveIndexRoot = hash_tree_root(
        get_active_validator_indices(state, getConstants().getGenesisEpoch()));

    for (EpochNumber index : getConstants().getLatestActiveIndexRootsLength().iterateFrom(EpochNumber.ZERO)) {
      state.getLatestActiveIndexRoots().set(index, genesisActiveIndexRoot);
    }

    return state.createImmutable();
  }
}
