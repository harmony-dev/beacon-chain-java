package org.ethereum.beacon.consensus.transition;

import static java.util.Collections.nCopies;

import java.util.List;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.CrosslinkRecord;
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
public class InitialStateTransition implements StateTransition<BeaconStateEx> {

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
    return apply(block, null);
  }

  @Override
  public BeaconStateEx apply(BeaconBlock block, BeaconStateEx state) {
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
    initialState.setPreviousEpochStartShard(chainSpec.getGenesisStartShard());
    initialState.setCurrentEpochStartShard(chainSpec.getGenesisStartShard());
    initialState.setPreviousCalculationEpoch(chainSpec.getGenesisEpoch());
    initialState.setCurrentCalculationEpoch(chainSpec.getGenesisEpoch());
    initialState.setPreviousEpochSeed(Hash32.ZERO);
    initialState.setCurrentEpochSeed(Hash32.ZERO);

    // Finality
    initialState.setPreviousJustifiedEpoch(chainSpec.getGenesisEpoch());
    initialState.setJustifiedEpoch(chainSpec.getGenesisEpoch());
    initialState.setJustificationBitfield(Bitfield64.ZERO);
    initialState.setFinalizedEpoch(chainSpec.getGenesisEpoch());

    // Recent state
    initialState.getLatestCrosslinks().addAll(
            nCopies(chainSpec.getShardCount().getIntValue(), CrosslinkRecord.EMPTY));
    initialState.getLatestBlockRoots().addAll(
            nCopies(chainSpec.getLatestBlockRootsLength().getIntValue(), Hash32.ZERO));
    initialState.getLatestIndexRoots().addAll(
            nCopies(chainSpec.getLatestIndexRootsLength().getIntValue(), Hash32.ZERO));
    initialState.getLatestPenalizedBalances().addAll(
            nCopies(chainSpec.getLatestPenalizedExitLength().getIntValue(), Gwei.ZERO));
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

    for (EpochNumber index : chainSpec.getLatestIndexRootsLength().iterateFromZero()) {
      initialState.getLatestIndexRoots().set(index, genesis_active_index_root);
    }

    initialState.setCurrentEpochSeed(
        specHelpers.generate_seed(initialState, chainSpec.getGenesisEpoch()));

    BeaconState validatorsState = initialState.createImmutable();
    Hash32 genesisBlockHash = BeaconBlocks.createGenesis(chainSpec)
        .withStateRoot(validatorsState.getHash()).getHash();
    return new BeaconStateEx(validatorsState, genesisBlockHash);
  }
}
