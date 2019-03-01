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
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.pow.DepositContract;
import tech.pegasys.artemis.ethereum.core.Hash32;

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
  private final ChainSpec chainSpec;
  private final SpecHelpers specHelpers;

  public InitialStateTransition(DepositContract.ChainStart depositContractStart,
      SpecHelpers specHelpers) {
    this.depositContractStart = depositContractStart;
    this.specHelpers = specHelpers;
    this.chainSpec = specHelpers.getChainSpec();
  }

  public BeaconStateEx apply(BeaconBlock block) {
    return apply(null, block);
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx state, BeaconBlock block) {
    assert block.getSlot().equals(chainSpec.getGenesisSlot());

    MutableBeaconState initialState = BeaconState.getEmpty().createMutableCopy();

    // Misc
    initialState.setSlot(chainSpec.getGenesisSlot());
    initialState.setGenesisTime(depositContractStart.getTime());
    initialState.setForkData(
            new ForkData(
                chainSpec.getGenesisForkVersion(),
                chainSpec.getGenesisForkVersion(),
                chainSpec.getGenesisEpoch()));

    // Validator registry
    initialState.getValidatorRegistry().clear();
    initialState.getValidatorBalances().clear();
    initialState.setValidatorRegistryUpdateEpoch(chainSpec.getGenesisEpoch());

    // Randomness and committees
    initialState.getLatestRandaoMixes().addAll(
            nCopies(chainSpec.getLatestRandaoMixesLength().getIntValue(), Hash32.ZERO));
    initialState.setPreviousShufflingStartShard(chainSpec.getGenesisStartShard());
    initialState.setCurrentShufflingStartShard(chainSpec.getGenesisStartShard());
    initialState.setPreviousShufflingEpoch(chainSpec.getGenesisEpoch());
    initialState.setCurrentShufflingEpoch(chainSpec.getGenesisEpoch());
    initialState.setPreviousShufflingSeed(Hash32.ZERO);
    initialState.setCurrentShufflingSeed(Hash32.ZERO);

    // Finality
    initialState.setPreviousJustifiedEpoch(chainSpec.getGenesisEpoch());
    initialState.setJustifiedEpoch(chainSpec.getGenesisEpoch());
    initialState.setJustificationBitfield(Bitfield64.ZERO);
    initialState.setFinalizedEpoch(chainSpec.getGenesisEpoch());

    // Recent state
    initialState.getLatestCrosslinks().addAll(
            nCopies(chainSpec.getShardCount().getIntValue(), Crosslink.EMPTY));
    initialState.getLatestBlockRoots().addAll(
            nCopies(chainSpec.getLatestBlockRootsLength().getIntValue(), Hash32.ZERO));
    initialState.getLatestActiveIndexRoots().addAll(
            nCopies(chainSpec.getLatestActiveIndexRootsLength().getIntValue(), Hash32.ZERO));
    initialState.getLatestSlashedBalances().addAll(
            nCopies(chainSpec.getSlashedExitLength().getIntValue(), Gwei.ZERO));
    initialState.getLatestAttestations().clear();
    initialState.getBatchedBlockRoots().clear();

    // PoW receipt root
    initialState.setLatestEth1Data(depositContractStart.getEth1Data());
    initialState.getEth1DataVotes().clear();

    // handle initial deposits and activations
    final List<Deposit> initialDeposits = depositContractStart.getInitialDeposits();

    initialDeposits.forEach(
        deposit -> {
          DepositData depositData = deposit.getDepositData();
          DepositInput depositInput = depositData.getDepositInput();
          ValidatorIndex index = specHelpers.process_deposit(initialState,
              depositInput.getPubKey(),
              depositData.getAmount(),
              depositInput.getProofOfPossession(),
              depositInput.getWithdrawalCredentials()
              );
        });

    for (ValidatorIndex validatorIndex :
        initialState.getValidatorRegistry().size().iterateFromZero()) {

      Gwei balance = specHelpers.get_effective_balance(initialState, validatorIndex);

      if (balance.greaterEqual(chainSpec.getMaxDepositAmount())) {
        specHelpers.activate_validator(initialState, validatorIndex, true);
      }
    }

    Hash32 genesis_active_index_root = specHelpers.hash_tree_root(
        specHelpers.get_active_validator_indices(
            initialState.getValidatorRegistry(), chainSpec.getGenesisEpoch()));

    for (EpochNumber index : chainSpec.getLatestActiveIndexRootsLength().iterateFromZero()) {
      initialState.getLatestActiveIndexRoots().set(index, genesis_active_index_root);
    }

    initialState.setCurrentShufflingSeed(
        specHelpers.generate_seed(initialState, chainSpec.getGenesisEpoch()));

    BeaconState validatorsState = initialState.createImmutable();
    BeaconBlock genesisBlock = block.withStateRoot(specHelpers.hash_tree_root(validatorsState));

    BeaconStateExImpl ret = new BeaconStateExImpl(
        validatorsState, specHelpers.hash_tree_root(genesisBlock), TransitionType.INITIAL);

    logger.debug(() -> "Slot transition result state: (" +
        specHelpers.hash_tree_root(ret).toStringShort() + ") " + ret.toString(chainSpec));

    return ret;
  }
}
