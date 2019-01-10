package org.ethereum.beacon.consensus.transition;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;

import java.util.List;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.state.ValidatorRegistryUpdater;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconChainSpec.Genesis;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.BeaconState.Builder;
import org.ethereum.beacon.core.Epoch;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.CrosslinkRecord;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.ShardCommittees;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Produces initial beacon state.
 *
 * <p>Requires input {@code block} to be a Genesis block, {@code state} parameter is ignored.
 * Preferred input for {@code state} parameter is {@link BeaconState#EMPTY}.
 *
 * <p>Uses {@link DepositContract} to fetch registration data from the PoW chain.
 *
 * @see DepositContract
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#on-startup">On
 *     startup in the spec</a>
 */
public class InitialStateTransition implements StateTransition<BeaconState> {

  private DepositContract depositContract;
  private ChainSpec chainSpec;

  public InitialStateTransition(DepositContract depositContract, ChainSpec chainSpec) {
    this.depositContract = depositContract;
    this.chainSpec = chainSpec;
  }

  @Override
  public BeaconState apply(BeaconBlock block, BeaconState state) {
    assert block.isGenesis();

    ChainStart chainStart = depositContract.getChainStart();

    Builder builder = BeaconState.Builder.createEmpty();

    // Misc
    builder
        .withSlot(Genesis.SLOT)
        .withGenesisTime(chainStart.getTime())
        .withForkData(new ForkData(Genesis.FORK_VERSION, Genesis.FORK_VERSION, Genesis.SLOT));

    // Validator registry
    builder
        .withValidatorRegistry(emptyList())
        .withValidatorBalances(emptyList())
        .withValidatorRegistryLatestChangeSlot(Genesis.SLOT)
        .withValidatorRegistryExitCount(UInt64.ZERO)
        .withValidatorRegistryDeltaChainTip(Hash32.ZERO);

    // Randomness and committees
    builder
        .withLatestRandaoMixes(
            nCopies(chainSpec.getLatestRandaoMixesLength().getIntValue(), Hash32.ZERO))
        .withLatestVdfOutputs(
            nCopies(
                chainSpec.getLatestRandaoMixesLength().getIntValue() / Epoch.LENGTH, Hash32.ZERO))
        .withShardCommitteesAtSlots(ShardCommittees.EMPTY);

    // Proof of custody
    builder.withCustodyChallenges(emptyList());

    // Finality
    builder
        .withPreviousJustifiedSlot(Genesis.SLOT)
        .withJustifiedSlot(Genesis.SLOT)
        .withJustificationBitfield(UInt64.ZERO)
        .withFinalizedSlot(Genesis.SLOT);

    // Recent state
    builder
        .withLatestCrosslinks(
            nCopies(chainSpec.getShardCount().getIntValue(), CrosslinkRecord.EMPTY))
        .withLatestBlockRoots(
            nCopies(chainSpec.getLatestBlockRootsLength().getIntValue(), Hash32.ZERO))
        .withLatestPenalizedExitBalances(
            nCopies(chainSpec.getLatestPenalizedExitLength().getIntValue(), UInt64.ZERO))
        .withLatestAttestations(emptyList())
        .withBatchedBlockRoots(emptyList());

    // PoW receipt root
    builder.withLatestDepositRoot(chainStart.getReceiptRoot()).withDepositRootVotes(emptyList());

    BeaconState initialState = builder.build();

    // handle initial deposits and activations
    final List<Deposit> initialDeposits = depositContract.getInitialDeposits();
    final ValidatorRegistryUpdater registryUpdater =
        ValidatorRegistryUpdater.fromState(initialState);

    initialDeposits.forEach(
        deposit -> {
          UInt24 index = registryUpdater.processDeposit(deposit);
          UInt64 balance = registryUpdater.getEffectiveBalance(index);

          // initial validators must have a strict deposit value
          if (DepositContract.MAX_DEPOSIT.toGWei().equals(balance)) {
            registryUpdater.activate(index);
          }
        });

    return registryUpdater.applyTo(initialState);
  }
}
