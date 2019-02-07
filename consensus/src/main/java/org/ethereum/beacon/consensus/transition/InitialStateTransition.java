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
    assert block.getSlot() == chainSpec.getGenesisSlot();

    MutableBeaconState initialState = BeaconState.getEmpty().createMutableCopy();

    // Misc
    initialState.setSlot(chainSpec.getGenesisSlot());
    initialState.setGenesisTime(depositContractStart.getTime());
    initialState.setForkData(
            new ForkData(
                chainSpec.getGenesisForkVersion(),
                chainSpec.getGenesisForkVersion(),
                chainSpec.getGenesisSlot()));

    // Validator registry
    initialState.getValidatorRegistry().clear();
    initialState.getValidatorBalances().clear();
    initialState.setValidatorRegistryLatestChangeSlot(chainSpec.getGenesisSlot());
    initialState.setValidatorRegistryExitCount(UInt64.ZERO);
    initialState.setValidatorRegistryDeltaChainTip(Hash32.ZERO);

    // Randomness and committees
    initialState.getLatestRandaoMixes().addAll(
            nCopies(chainSpec.getLatestRandaoMixesLength().getIntValue(), Hash32.ZERO));
    initialState.getLatestVdfOutputs().addAll(
            nCopies(
                chainSpec
                    .getLatestRandaoMixesLength()
                    .dividedBy(chainSpec.getEpochLength())
                    .getIntValue(),
                Hash32.ZERO));
    initialState.setPreviousEpochStartShard(chainSpec.getGenesisStartShard());
    initialState.setCurrentEpochStartShard(chainSpec.getGenesisStartShard());
    initialState.setPreviousEpochCalculationSlot(chainSpec.getGenesisSlot());
    initialState.setCurrentEpochCalculationSlot(chainSpec.getGenesisSlot());
    initialState.setPreviousEpochRandaoMix(Hash32.ZERO);
    initialState.setCurrentEpochRandaoMix(Hash32.ZERO);

    // Proof of custody
    initialState.getCustodyChallenges().clear();

    // Finality
    initialState.setPreviousJustifiedSlot(chainSpec.getGenesisSlot());
    initialState.setJustifiedSlot(chainSpec.getGenesisSlot());
    initialState.setJustificationBitfield(Bitfield64.ZERO);
    initialState.setFinalizedSlot(chainSpec.getGenesisSlot());

    // Recent state
    initialState.getLatestCrosslinks().addAll(
            nCopies(chainSpec.getShardCount().getIntValue(), CrosslinkRecord.EMPTY));
    initialState.getLatestBlockRoots().addAll(
            nCopies(chainSpec.getLatestBlockRootsLength().getIntValue(), Hash32.ZERO));
    initialState.getLatestPenalizedExitBalances().addAll(
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
              depositData.getValue(),
              depositInput.getProofOfPossession(),
              depositInput.getWithdrawalCredentials()
              );
          Gwei balance = specHelpers.get_effective_balance(initialState, index);

          // initial validators must have a strict deposit value
          if (chainSpec.getMaxDeposit().lessEqual(balance)) {
            specHelpers.activate_validator(initialState, index, true);
          }
        });


    BeaconState validatorsState = initialState.createImmutable();

    BeaconBlock genesisBlock = BeaconBlocks.createGenesis(chainSpec)
        .withStateRoot(specHelpers.hash_tree_root(validatorsState));

    return new BeaconStateEx(validatorsState, specHelpers.hash_tree_root(genesisBlock));
  }
}
