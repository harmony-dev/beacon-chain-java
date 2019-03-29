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
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * On genesis part.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/core/0_beacon-chain.md#on-genesis">On
 *     genesis</a> in the spec.
 */
public interface OnGenesis extends HelperFunction {

  /*
   """
   Get an empty ``BeaconBlock``.
   """
  */
  default BeaconBlock get_empty_block() {
    BeaconBlockBody body =
        new BeaconBlockBody(
            getConstants().getEmptySignature(),
            new Eth1Data(Hash32.ZERO, Hash32.ZERO),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList());
    return new BeaconBlock(
        getConstants().getGenesisSlot(), Hash32.ZERO, Hash32.ZERO, body, getConstants().getEmptySignature());
  }

  /*
   """
   Get the genesis ``BeaconState``.
   """
  */
  default BeaconState get_genesis_beacon_state(
      List<Deposit> genesisValidatorDeposits, Time genesisTime, Eth1Data genesisEth1Data) {
    MutableBeaconState state = BeaconState.getEmpty().createMutableCopy();

    // Misc
    state.setSlot(getConstants().getGenesisSlot());
    state.setGenesisTime(genesisTime);
    state.setFork(new Fork(
        int_to_bytes4(getConstants().getGenesisForkVersion()),
        int_to_bytes4(getConstants().getGenesisForkVersion()),
        getConstants().getGenesisEpoch()));

    // Validator registry
    state.getValidatorRegistry().clear();
    state.getValidatorBalances().clear();
    state.setValidatorRegistryUpdateEpoch(getConstants().getGenesisEpoch());

    // Randomness and committees
    state.getLatestRandaoMixes().addAll(
        nCopies(getConstants().getLatestRandaoMixesLength().getIntValue(), Hash32.ZERO));
    state.setPreviousShufflingStartShard(getConstants().getGenesisStartShard());
    state.setCurrentShufflingStartShard(getConstants().getGenesisStartShard());
    state.setPreviousShufflingEpoch(getConstants().getGenesisEpoch());
    state.setCurrentShufflingEpoch(getConstants().getGenesisEpoch());
    state.setPreviousShufflingSeed(Hash32.ZERO);
    state.setCurrentShufflingSeed(Hash32.ZERO);

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
    state.getPreviousEpochCrosslinks().addAll(
        nCopies(getConstants().getShardCount().getIntValue(),
            new Crosslink(getConstants().getGenesisEpoch(), Hash32.ZERO)));
    state.getCurrentEpochCrosslinks().addAll(
        nCopies(getConstants().getShardCount().getIntValue(),
            new Crosslink(getConstants().getGenesisEpoch(), Hash32.ZERO)));
    state.getLatestBlockRoots().addAll(
        nCopies(getConstants().getSlotsPerHistoricalRoot().getIntValue(), Hash32.ZERO));
    state.getLatestStateRoots().addAll(
        nCopies(getConstants().getSlotsPerHistoricalRoot().getIntValue(), Hash32.ZERO));
    state.getLatestActiveIndexRoots().addAll(
        nCopies(getConstants().getLatestActiveIndexRootsLength().getIntValue(), Hash32.ZERO));
    state.getLatestSlashedBalances().addAll(
        nCopies(getConstants().getLatestSlashedExitLength().getIntValue(), Gwei.ZERO));
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
      if (get_effective_balance(state, validatorIndex).greaterEqual(getConstants().getMaxDepositAmount())) {
        activate_validator(state, validatorIndex, true);
      }
    }

    Hash32 genesisActiveIndexRoot = hash_tree_root(
        get_active_validator_indices(state.getValidatorRegistry(), getConstants().getGenesisEpoch()));

    for (EpochNumber index : getConstants().getLatestActiveIndexRootsLength().iterateFrom(EpochNumber.ZERO)) {
      state.getLatestActiveIndexRoots().set(index, genesisActiveIndexRoot);
    }
    state.setCurrentShufflingSeed(generate_seed(state, getConstants().getGenesisEpoch()));

    return state.createImmutable();
  }
}
