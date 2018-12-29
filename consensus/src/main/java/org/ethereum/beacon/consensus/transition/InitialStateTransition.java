package org.ethereum.beacon.consensus.transition;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;

import java.util.List;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.state.ValidatorRegistryUpdater;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.BeaconState.Builder;
import org.ethereum.beacon.core.Epoch;
import org.ethereum.beacon.core.Slot;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.state.CrosslinkRecord;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.PersistentCommittees;
import org.ethereum.beacon.core.state.ShardCommittees;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public class InitialStateTransition implements StateTransition<BeaconState> {

  private DepositContract depositContract;

  public InitialStateTransition(DepositContract depositContract) {
    this.depositContract = depositContract;
  }

  @Override
  public BeaconState apply(BeaconBlock block, BeaconState state) {
    assert block.isGenesis();

    ChainStart chainStart = depositContract.getChainStart();

    Builder builder = BeaconState.Builder.createEmpty();

    // Misc
    builder
        .withSlot(Slot.INITIAL_NUMBER)
        .withGenesisTime(chainStart.getTime())
        .withForkData(
            new ForkData(Fork.INITIAL_VERSION, Fork.INITIAL_VERSION, Slot.INITIAL_NUMBER));

    // Validator registry
    builder
        .withValidatorRegistry(emptyList())
        .withValidatorBalances(emptyList())
        .withValidatorRegistryLatestChangeSlot(Slot.INITIAL_NUMBER)
        .withValidatorRegistryExitCount(UInt64.ZERO)
        .withValidatorRegistryDeltaChainTip(Hash32.ZERO);

    // Randomness and committees
    builder
        .withLatestRandaoMixes(nCopies(BeaconState.LATEST_RANDAO_MIXES_LENGTH, Hash32.ZERO))
        .withLatestVdfOutputs(
            nCopies(BeaconState.LATEST_RANDAO_MIXES_LENGTH / Epoch.LENGTH, Hash32.ZERO))
        .withShardCommitteesAtSlots(ShardCommittees.EMPTY)
        .withPersistentCommittees(PersistentCommittees.EMPTY)
        .withPersistentCommitteeReassignments(emptyList());

    // Proof of custody
    builder.withPocChallenges(emptyList());

    // Finality
    builder
        .withPreviousJustifiedSlot(Slot.INITIAL_NUMBER)
        .withJustifiedSlot(Slot.INITIAL_NUMBER)
        .withJustificationBitfield(UInt64.ZERO)
        .withFinalizedSlot(Slot.INITIAL_NUMBER);

    // Recent state
    builder
        .withLatestCrosslinks(nCopies(BeaconChainSpec.SHARD_COUNT, CrosslinkRecord.EMPTY))
        .withLatestBlockRoots(nCopies(BeaconState.LATEST_BLOCK_ROOTS_LENGTH, Hash32.ZERO))
        .withLatestPenalizedExitBalances(emptyList())
        .withLatestAttestations(emptyList())
        .withBatchedBlockRoots(emptyList());

    // PoW receipt root
    builder
        .withProcessedPowReceiptRoot(chainStart.getReceiptRoot())
        .withCandidatePowReceiptRoots(emptyList());

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
