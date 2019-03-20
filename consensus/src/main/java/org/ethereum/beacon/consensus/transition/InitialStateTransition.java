package org.ethereum.beacon.consensus.transition;

import static java.util.Collections.nCopies;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.pow.DepositContract;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Produces initial beacon state.
 *
 * <p>Requires input {@code block} to be a Genesis block, {@code state} parameter is ignored.
 * Preferred input for {@code state} parameter is {@link BeaconState#getEmpty()}.
 *
 * <p>Uses {@link DepositContract} to fetch registration data from the PoW chain.
 *
 * @see DepositContract
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#on-startup">On
 *     startup in the spec</a>
 */
public class InitialStateTransition implements BlockTransition<BeaconStateEx> {
  private static final Logger logger = LogManager.getLogger(InitialStateTransition.class);

  private final DepositContract.ChainStart depositContractStart;
  private final SpecHelpers spec;

  public InitialStateTransition(DepositContract.ChainStart depositContractStart,
      SpecHelpers spec) {
    this.depositContractStart = depositContractStart;
    this.spec = spec;
  }

  public BeaconStateEx apply(BeaconBlock block) {
    return apply(null, block);
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx state, BeaconBlock block) {
    assert block.getSlot().equals(spec.getConstants().getGenesisSlot());

    MutableBeaconState initialState = BeaconState.getEmpty().createMutableCopy();

    // Misc
    initialState.setSlot(spec.getConstants().getGenesisSlot());
    initialState.setGenesisTime(depositContractStart.getTime());
    initialState.setFork(
            new Fork(
                spec.int_to_bytes4(spec.getConstants().getGenesisForkVersion()),
                spec.int_to_bytes4(spec.getConstants().getGenesisForkVersion()),
                spec.getConstants().getGenesisEpoch()));

    // Validator registry
    initialState.getValidatorRegistry().clear();
    initialState.getValidatorBalances().clear();
    initialState.setValidatorRegistryUpdateEpoch(spec.getConstants().getGenesisEpoch());

    // Randomness and committees
    initialState.getLatestRandaoMixes().addAll(
            nCopies(spec.getConstants().getLatestRandaoMixesLength().getIntValue(), Hash32.ZERO));
    initialState.setPreviousShufflingStartShard(spec.getConstants().getGenesisStartShard());
    initialState.setCurrentShufflingStartShard(spec.getConstants().getGenesisStartShard());
    initialState.setPreviousShufflingEpoch(spec.getConstants().getGenesisEpoch());
    initialState.setCurrentShufflingEpoch(spec.getConstants().getGenesisEpoch());
    initialState.setPreviousShufflingSeed(Hash32.ZERO);
    initialState.setCurrentShufflingSeed(Hash32.ZERO);

    // Finality
    initialState.setPreviousJustifiedEpoch(spec.getConstants().getGenesisEpoch());
    initialState.setJustifiedEpoch(spec.getConstants().getGenesisEpoch());
    initialState.setJustificationBitfield(Bitfield64.ZERO);
    initialState.setFinalizedEpoch(spec.getConstants().getGenesisEpoch());

    // Recent state
    initialState.getLatestCrosslinks().addAll(
            nCopies(spec.getConstants().getShardCount().getIntValue(), Crosslink.EMPTY));
    initialState.getLatestBlockRoots().addAll(
            nCopies(spec.getConstants().getSlotsPerHistoricalRoot().getIntValue(), Hash32.ZERO));
    initialState.getLatestActiveIndexRoots().addAll(
            nCopies(spec.getConstants().getLatestActiveIndexRootsLength().getIntValue(), Hash32.ZERO));
    initialState.getLatestSlashedBalances().addAll(
            nCopies(spec.getConstants().getLatestSlashedExitLength().getIntValue(), Gwei.ZERO));
    initialState.getLatestAttestations().clear();
    initialState.getBatchedBlockRoots().clear();

    // PoW receipt root
    initialState.setLatestEth1Data(depositContractStart.getEth1Data());
    initialState.getEth1DataVotes().clear();

    // handle initial deposits and activations
    final List<Deposit> initialDeposits = depositContractStart.getInitialDeposits();
    initialState.setDepositIndex(UInt64.valueOf(initialDeposits.size()));

    for (Deposit deposit : initialDeposits) {
      spec.process_deposit(initialState, deposit);
    }

    for (ValidatorIndex validatorIndex :
        initialState.getValidatorRegistry().size().iterateFromZero()) {

      Gwei balance = spec.get_effective_balance(initialState, validatorIndex);

      if (balance.greaterEqual(spec.getConstants().getMaxDepositAmount())) {
        spec.activate_validator(initialState, validatorIndex, true);
      }
    }

    Hash32 genesis_active_index_root = spec.hash_tree_root(
        spec.get_active_validator_indices(
            initialState.getValidatorRegistry(), spec.getConstants().getGenesisEpoch()));

    for (EpochNumber index : spec.getConstants().getLatestActiveIndexRootsLength().iterateFromZero()) {
      initialState.getLatestActiveIndexRoots().set(index, genesis_active_index_root);
    }

    initialState.setCurrentShufflingSeed(
        spec.generate_seed(initialState, spec.getConstants().getGenesisEpoch()));

    BeaconState validatorsState = initialState.createImmutable();
    BeaconBlock genesisBlock = block.withStateRoot(spec.hash_tree_root(validatorsState));

    BeaconStateExImpl ret = new BeaconStateExImpl(
        validatorsState, spec.hash_tree_root(genesisBlock), TransitionType.INITIAL);

    logger.debug(() -> "Slot transition result state: (" +
        spec.hash_tree_root(ret).toStringShort() + ") " + ret.toString(spec.getConstants()));

    return ret;
  }
}
