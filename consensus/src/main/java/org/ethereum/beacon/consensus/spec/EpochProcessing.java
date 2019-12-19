package org.ethereum.beacon.consensus.spec;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.state.HistoricalBatch;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.javatuples.Pair;
import tech.pegasys.artemis.util.collections.Bitvector;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

/**
 * Epoch processing part.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.9.2/specs/core/0_beacon-chain.md#epoch-processing">Epoch
 *     processing</a> in the spec.
 */
public interface EpochProcessing extends HelperFunction {

  /*
    def get_matching_source_attestations(state: BeaconState, epoch: Epoch) -> List[PendingAttestation]:
      assert epoch in (get_current_epoch(state), get_previous_epoch(state))
      return state.current_epoch_attestations if epoch == get_current_epoch(state) else state.previous_epoch_attestations
   */
  default List<PendingAttestation> get_matching_source_attestations(BeaconState state, EpochNumber epoch) {
    assertTrue(epoch.equals(get_current_epoch(state)) || epoch.equals(get_previous_epoch(state)));
    return epoch.equals(get_current_epoch(state)) ?
        state.getCurrentEpochAttestations().listCopy() : state.getPreviousEpochAttestations().listCopy();
  }

  /*
    def get_matching_target_attestations(state: BeaconState, epoch: Epoch) -> Sequence[PendingAttestation]:
      return [
          a for a in get_matching_source_attestations(state, epoch)
          if a.data.target.root == get_block_root(state, epoch)
      ]
   */
  default List<PendingAttestation> get_matching_target_attestations(BeaconState state, EpochNumber epoch) {
    return get_matching_source_attestations(state, epoch).stream()
        .filter(a -> a.getData().getTarget().getRoot().equals(get_block_root(state, epoch)))
        .collect(toList());
  }

  /*
    def get_matching_head_attestations(state: BeaconState, epoch: Epoch) -> List[PendingAttestation]:
      return [
          a for a in get_matching_source_attestations(state, epoch)
          if a.data.beacon_block_root == get_block_root_at_slot(state, a.data.slot)
      ]
   */
  default List<PendingAttestation> get_matching_head_attestations(BeaconState state, EpochNumber epoch) {
    return get_matching_source_attestations(state, epoch).stream()
        .filter(a -> a.getData().getBeaconBlockRoot().equals(
            get_block_root_at_slot(state, a.getData().getSlot())))
        .collect(toList());
  }

  /*
    def get_unslashed_attesting_indices(state: BeaconState,
                                    attestations: Sequence[PendingAttestation]) -> Set[ValidatorIndex]:
      output = set()  # type: Set[ValidatorIndex]
      for a in attestations:
          output = output.union(get_attesting_indices(state, a.data, a.aggregation_bits))
      return set(filter(lambda index: not state.validators[index].slashed, output))
   */
  default List<ValidatorIndex> get_unslashed_attesting_indices(BeaconState state, List<PendingAttestation> attestations) {
    return attestations.stream()
        .flatMap(a -> get_attesting_indices(state, a.getData(), a.getAggregationBits()).stream())
        .distinct()
        .filter(i -> !state.getValidators().get(i).getSlashed())
        .collect(Collectors.toList());
  }

  /*
    def get_attesting_balance(state: BeaconState, attestations: List[PendingAttestation]) -> Gwei:
      return get_total_balance(state, get_unslashed_attesting_indices(state, attestations))
   */
  default Gwei get_attesting_balance(BeaconState state, List<PendingAttestation> attestations) {
    return get_total_balance(state, get_unslashed_attesting_indices(state, attestations));
  }

  /*
    def process_justification_and_finalization(state: BeaconState) -> None:
      if get_current_epoch(state) <= GENESIS_EPOCH + 1:
          return
   */
  default void process_justification_and_finalization(MutableBeaconState state) {
    if (get_current_epoch(state).lessEqual(getConstants().getGenesisEpoch().increment())) {
      return;
    }

    EpochNumber previous_epoch = get_previous_epoch(state);
    EpochNumber current_epoch = get_current_epoch(state);
    Checkpoint old_previous_justified_checkpoint = state.getPreviousJustifiedCheckpoint();
    Checkpoint old_current_justified_checkpoint = state.getCurrentJustifiedCheckpoint();

    /* # Process justifications
       state.previous_justified_checkpoint = state.current_justified_checkpoint
       state.justification_bits[1:] = state.justification_bits[:-1]
       state.justification_bits[0] = 0b0 */
    state.setPreviousJustifiedCheckpoint(state.getCurrentJustifiedCheckpoint());
    state.setJustificationBits(state.getJustificationBits().shl(1));

    /* matching_target_attestations = get_matching_target_attestations(state, previous_epoch)  # Previous epoch
       if get_attesting_balance(state, matching_target_attestations) * 3 >= get_total_active_balance(state) * 2:
          state.current_justified_checkpoint = Checkpoint(epoch=previous_epoch,
                                                        root=get_block_root(state, previous_epoch))
       state.justification_bits[1] = 0b1 */
    List<PendingAttestation> matching_target_attestations =
        get_matching_target_attestations(state, previous_epoch);
    if (get_attesting_balance(state, matching_target_attestations).times(3)
        .greaterEqual(get_total_active_balance(state).times(2))) {
      state.setCurrentJustifiedCheckpoint(
          new Checkpoint(previous_epoch, get_block_root(state, previous_epoch)));
      state.setJustificationBits(state.getJustificationBits().setBit(1, 0b1));
    }

    /* matching_target_attestations = get_matching_target_attestations(state, current_epoch)  # Current epoch
       if get_attesting_balance(state, matching_target_attestations) * 3 >= get_total_active_balance(state) * 2:
           state.current_justified_checkpoint = Checkpoint(epoch=current_epoch,
                                                        root=get_block_root(state, current_epoch))
           state.justification_bits[0] = 0b1 */
    matching_target_attestations =
        get_matching_target_attestations(state, current_epoch);
    if (get_attesting_balance(state, matching_target_attestations).times(3)
        .greaterEqual(get_total_active_balance(state).times(2))) {
      state.setCurrentJustifiedCheckpoint(
          new Checkpoint(current_epoch, get_block_root(state, current_epoch)));
      state.setJustificationBits(state.getJustificationBits().setBit(0, 0b1));
    }

    /* # Process finalizations
       bits = state.justification_bits */
    Bitvector bits = state.getJustificationBits();

    /* # The 2nd/3rd/4th most recent epochs are justified, the 2nd using the 4th as source
       if all(bits[1:4]) and old_previous_justified_checkpoint.epoch + 3 == current_epoch:
           state.finalized_checkpoint = old_previous_justified_checkpoint */
    if ((bits.getValue() >>> 1) % 8 == 0b111
        && old_previous_justified_checkpoint.getEpoch().plus(3).equals(current_epoch)) {
      state.setFinalizedCheckpoint(old_previous_justified_checkpoint);
    }

    /* # The 2nd/3rd most recent epochs are justified, the 2nd using the 3rd as source
       if all(bits[1:3]) and old_previous_justified_checkpoint.epoch + 2 == current_epoch:
           state.finalized_checkpoint = old_previous_justified_checkpoint */
    if ((bits.getValue() >>> 1) % 4 == 0b11
        && old_previous_justified_checkpoint.getEpoch().plus(2).equals(current_epoch)) {
      state.setFinalizedCheckpoint(old_previous_justified_checkpoint);
    }

    /* # The 1st/2nd/3rd most recent epochs are justified, the 1st using the 3rd as source
       if all(bits[0:3]) and old_current_justified_checkpoint.epoch + 2 == current_epoch:
           state.finalized_checkpoint = old_current_justified_checkpoint */
    if (bits.getValue() % 8 == 0b111
        && old_current_justified_checkpoint.getEpoch().plus(2).equals(current_epoch)) {
      state.setFinalizedCheckpoint(old_current_justified_checkpoint);
    }

    /* # The 1st/2nd most recent epochs are justified, the 1st using the 2nd as source
       if all(bits[0:2]) and old_current_justified_checkpoint.epoch + 1 == current_epoch:
           state.finalized_checkpoint = old_current_justified_checkpoint */
    if (bits.getValue() % 4 == 0b11
        && old_current_justified_checkpoint.getEpoch().plus(1).equals(current_epoch)) {
      state.setFinalizedCheckpoint(old_current_justified_checkpoint);
    }
  }

  /*
    def get_base_reward(state: BeaconState, index: ValidatorIndex) -> Gwei:
      total_balance = get_total_active_balance(state)
      effective_balance = state.validator_registry[index].effective_balance
      return effective_balance * BASE_REWARD_FACTOR // integer_squareroot(total_balance) // BASE_REWARDS_PER_EPOCH
   */
  default Gwei get_base_reward(BeaconState state, ValidatorIndex index) {
    UInt64 total_balance = get_total_active_balance(state);
    Gwei effective_balance = state.getValidators().get(index).getEffectiveBalance();
    return effective_balance.times(getConstants().getBaseRewardFactor())
        .dividedBy(integer_squareroot(total_balance))
        .dividedBy(getConstants().getBaseRewardsPerEpoch());
  }

  /*
    def get_attestation_deltas(state: BeaconState) -> Tuple[List[Gwei], List[Gwei]]:
   */
  default Gwei[][] get_attestation_deltas(BeaconState state) {
    EpochNumber previous_epoch = get_previous_epoch(state);
    Gwei total_balance = get_total_active_balance(state);
    Gwei[] rewards = new Gwei[state.getValidators().size().getIntValue()];
    Gwei[] penalties = new Gwei[state.getValidators().size().getIntValue()];
    Arrays.fill(rewards, Gwei.ZERO);
    Arrays.fill(penalties, Gwei.ZERO);

    List<ValidatorIndex> eligible_validator_indices = new ArrayList<>();
    for (ValidatorIndex index : state.getValidators().size()) {
      ValidatorRecord validator = state.getValidators().get(index);
      if (is_active_validator(validator, previous_epoch)
          || (validator.getSlashed() && previous_epoch.increment().less(validator.getWithdrawableEpoch()))) {
        eligible_validator_indices.add(index);
      }
    }

    /* Micro-incentives for matching FFG source, FFG target, and head
        matching_source_attestations = get_matching_source_attestations(state, previous_epoch)
        matching_target_attestations = get_matching_target_attestations(state, previous_epoch)
        matching_head_attestations = get_matching_head_attestations(state, previous_epoch) */
    List<PendingAttestation> matching_source_attestations = get_matching_source_attestations(state, previous_epoch);
    List<PendingAttestation> matching_target_attestations = get_matching_target_attestations(state, previous_epoch);
    List<PendingAttestation> matching_head_attestations = get_matching_head_attestations(state, previous_epoch);

    /*  for attestations in (matching_source_attestations, matching_target_attestations, matching_head_attestations):
          unslashed_attesting_indices = get_unslashed_attesting_indices(state, attestations)
          attesting_balance = get_total_balance(state, unslashed_attesting_indices)
          for index in eligible_validator_indices:
              if index in unslashed_attesting_indices:
                  rewards[index] += get_base_reward(state, index) * attesting_balance // total_balance
              else:
                  penalties[index] += get_base_reward(state, index) */
    for (List<PendingAttestation> attestations :
        Arrays.asList(matching_source_attestations, matching_target_attestations, matching_head_attestations)) {
      List<ValidatorIndex> unslashed_attesting_indices = get_unslashed_attesting_indices(state, attestations);
      Gwei attesting_balance = get_total_balance(state, unslashed_attesting_indices);
      for (ValidatorIndex index : eligible_validator_indices) {
        if (unslashed_attesting_indices.contains(index)) {
          rewards[index.getIntValue()] = rewards[index.getIntValue()]
              .plus(get_base_reward(state, index).times(attesting_balance).dividedBy(total_balance));
        } else {
          penalties[index.getIntValue()] = penalties[index.getIntValue()]
              .plus(get_base_reward(state, index));
        }
      }
    }

    /* # Proposer and inclusion delay micro-rewards
       for index in get_unslashed_attesting_indices(state, matching_source_attestations):
          attestation = min([
              a for a in matching_source_attestations
              if index in get_attesting_indices(state, a.data, a.aggregation_bits)
          ], key=lambda a: a.inclusion_delay)
          proposer_reward = Gwei(get_base_reward(state, index) // PROPOSER_REWARD_QUOTIENT)
          rewards[attestation.proposer_index] += proposer_reward
          max_attester_reward = get_base_reward(state, index) - proposer_reward
          rewards[index] += Gwei(
              max_attester_reward
              * (SLOTS_PER_EPOCH + MIN_ATTESTATION_INCLUSION_DELAY - attestation.inclusion_delay)
              // SLOTS_PER_EPOCH
          ) */
    for (ValidatorIndex index : get_unslashed_attesting_indices(state, matching_source_attestations)) {
      PendingAttestation attestation =
          matching_source_attestations.stream()
              .filter(a -> get_attesting_indices(state, a.getData(), a.getAggregationBits()).contains(index))
              .min(Comparator.comparing(PendingAttestation::getInclusionDelay))
              .get();
      Gwei proposer_reward = get_base_reward(state, index).dividedBy(getConstants().getProposerRewardQuotient());
      rewards[attestation.getProposerIndex().getIntValue()] = rewards[attestation.getProposerIndex().getIntValue()]
          .plus(proposer_reward);
      Gwei max_attester_reward = get_base_reward(state, index).minus(proposer_reward);
      rewards[index.getIntValue()] = rewards[index.getIntValue()]
          .plus(max_attester_reward.dividedBy(attestation.getInclusionDelay()));
    }

    /* Inactivity penalty
      finality_delay = previous_epoch - state.finalized_epoch
      if finality_delay > MIN_EPOCHS_TO_INACTIVITY_PENALTY:
          matching_target_attesting_indices = get_unslashed_attesting_indices(state, matching_target_attestations)
          for index in eligible_validator_indices:
              penalties[index] += BASE_REWARDS_PER_EPOCH * get_base_reward(state, index)
              if index not in matching_target_attesting_indices:
                  penalties[index] += state.validator_registry[index].effective_balance * finality_delay // INACTIVITY_PENALTY_QUOTIENT */
    EpochNumber finality_delay = previous_epoch.minus(state.getFinalizedCheckpoint().getEpoch());
    if (finality_delay.greater(getConstants().getMinEpochsToInactivityPenalty())) {
      List<ValidatorIndex> matching_target_attesting_indices = get_unslashed_attesting_indices(state, matching_target_attestations);
      for (ValidatorIndex index : eligible_validator_indices) {
        penalties[index.getIntValue()] = penalties[index.getIntValue()]
            .plus(get_base_reward(state, index).times(getConstants().getBaseRewardsPerEpoch()));
        if (!matching_target_attesting_indices.contains(index)) {
          penalties[index.getIntValue()] = penalties[index.getIntValue()]
              .plus(state.getValidators().get(index).getEffectiveBalance()
                  .times(finality_delay).dividedBy(getConstants().getInactivityPenaltyQuotient()));
        }
      }
    }

    return new Gwei[][] { rewards, penalties };
  }

  /*
    def process_rewards_and_penalties(state: BeaconState) -> None:
      if get_current_epoch(state) == GENESIS_EPOCH:
          return

      rewards, penalties = get_attestation_deltas(state)
      for index in range(len(state.validators)):
          increase_balance(state, ValidatorIndex(index), rewards[index])
          decrease_balance(state, ValidatorIndex(index), penalties[index])   */
  default void process_rewards_and_penalties(MutableBeaconState state) {
    if (get_current_epoch(state).equals(getConstants().getGenesisEpoch())) {
      return;
    }

    Gwei[][] deltas = get_attestation_deltas(state);
    Gwei[] rewards = deltas[0], penalties = deltas[1];
    for (ValidatorIndex i : state.getValidators().size()) {
      increase_balance(state, i, rewards[i.getIntValue()]);
      decrease_balance(state, i, penalties[i.getIntValue()]);
    }
  }

  /*
    def process_registry_updates(state: BeaconState) -> None:
   */
  default List<ValidatorIndex> process_registry_updates(MutableBeaconState state) {
    /* Process activation eligibility and ejections
      for index, validator in enumerate(state.validator_registry):
        if is_eligible_for_activation_queue(validator):
            validator.activation_eligibility_epoch = get_current_epoch(state) + 1

        if is_active_validator(validator, get_current_epoch(state)) and validator.effective_balance <= EJECTION_BALANCE:
            initiate_validator_exit(state, ValidatorIndex(index)) */
    List<ValidatorIndex> ejected = new ArrayList<>();
    for (ValidatorIndex index : state.getValidators().size()) {
      ValidatorRecord validator = state.getValidators().get(index);
      if (is_eligible_for_activation_queue(validator)) {
        state.getValidators().update(index,
            v -> ValidatorRecord.Builder.fromRecord(v)
                .withActivationEligibilityEpoch(get_current_epoch(state).increment()).build());
      }

      if (is_active_validator(validator, get_current_epoch(state))
          && validator.getEffectiveBalance().lessEqual(getConstants().getEjectionBalance())) {
        initiate_validator_exit(state, index);
        ejected.add(index);
      }
    }

    /* Queue validators eligible for activation and not yet dequeued for activation
      activation_queue = sorted([
          index for index, validator in enumerate(state.validators)
          if is_eligible_for_activation(state, validator)
          # Order by the sequence of activation_eligibility_epoch setting and then index
      ], key=lambda index: (state.validators[index].activation_eligibility_epoch, index)) */
    List<Pair<ValidatorIndex, ValidatorRecord>> activation_queue = new ArrayList<>();
    for (ValidatorIndex index : state.getValidators().size()) {
      ValidatorRecord validator = state.getValidators().get(index);
      if (is_eligible_for_activation(state, validator)) {
        activation_queue.add(Pair.with(index, validator));
      }
    }
    activation_queue.sort(Comparator.comparing(p -> p.getValue1().getActivationEligibilityEpoch()));

    /* Dequeued validators for activation up to churn limit
      for index in activation_queue[:get_validator_churn_limit(state)]:
        validator = state.validators[index]
        validator.activation_epoch = compute_activation_exit_epoch(get_current_epoch(state)) */
    int limit = get_validator_churn_limit(state).getIntValue();
    List<Pair<ValidatorIndex, ValidatorRecord>> limited_activation_queue =
        activation_queue.size() > limit ? activation_queue.subList(0, limit) : activation_queue;

    for (Pair<ValidatorIndex, ValidatorRecord> p : limited_activation_queue) {
      if (p.getValue1().getActivationEpoch().equals(getConstants().getFarFutureEpoch())) {
        state.getValidators().update(p.getValue0(),
            v -> ValidatorRecord.Builder.fromRecord(v)
                .withActivationEpoch(compute_activation_exit_epoch(get_current_epoch(state)))
                .build());
      }
    }

    return ejected;
  }

  /*
    def process_slashings(state: BeaconState) -> None:
   */
  default void process_slashings(MutableBeaconState state) {
    /* epoch = get_current_epoch(state)
       total_balance = get_total_active_balance(state) */
    EpochNumber epoch = get_current_epoch(state);
    Gwei total_balance = get_total_active_balance(state);

    /* for index, validator in enumerate(state.validators):
        if validator.slashed and epoch + EPOCHS_PER_SLASHINGS_VECTOR // 2 == validator.withdrawable_epoch:
            increment = EFFECTIVE_BALANCE_INCREMENT  # Factored out from penalty numerator to avoid uint64 overflow
            penalty_numerator = ( validator.effective_balance // increment ) * min(sum(state.slashings) * 3, total_balance)
            penalty = penalty_numerator // total_balance * increment
            decrease_balance(state, ValidatorIndex(index), penalty) */
    for (ValidatorIndex index : state.getValidators().size()) {
      ValidatorRecord validator = state.getValidators().get(index);
      if (validator.getSlashed()
          && epoch.plus(getConstants().getEpochsPerSlashingsVector().half()).equals(validator.getWithdrawableEpoch())) {
        Gwei increment = getConstants().getEffectiveBalanceIncrement();
        Gwei stateSlashings = state.getSlashings().stream().reduce(Gwei::plus).orElse(Gwei.ZERO);
        Gwei penalty_numerator =
            validator
                .getEffectiveBalance()
                .dividedBy(increment)
                .times(UInt64s.min(stateSlashings.times(3), total_balance));
        Gwei penalty = penalty_numerator.dividedBy(total_balance).times(increment);
        decrease_balance(state, index, penalty);
      }
    }
  }

  /*
    def process_final_updates(state: BeaconState) -> None:
   */
  default void process_final_updates(MutableBeaconState state) {
    /* current_epoch = get_current_epoch(state)
       next_epoch = current_epoch + 1 */
    EpochNumber current_epoch = get_current_epoch(state);
    EpochNumber next_epoch = current_epoch.increment();

    /* Reset eth1 data votes
      if (state.slot + 1) % SLOTS_PER_ETH1_VOTING_PERIOD == 0:
          state.eth1_data_votes = [] */
    if (state.getSlot().increment().modulo(getConstants().getSlotsPerEth1VotingPeriod())
        .equals(SlotNumber.ZERO)) {
      state.getEth1DataVotes().clear();
    }

    /* Update effective balances with hysteresis
      for index, validator in enumerate(state.validator_registry):
          balance = state.balances[index]
          HALF_INCREMENT = EFFECTIVE_BALANCE_INCREMENT // 2
          if balance < validator.effective_balance or validator.effective_balance + 3 * HALF_INCREMENT < balance:
              validator.effective_balance = min(balance - balance % EFFECTIVE_BALANCE_INCREMENT, MAX_EFFECTIVE_BALANCE) */
    Gwei half_increment = getConstants().getEffectiveBalanceIncrement().dividedBy(2);
    for (ValidatorIndex index : state.getValidators().size()) {
      ValidatorRecord validator = state.getValidators().get(index);
      Gwei balance = state.getBalances().get(index);
      if (balance.less(validator.getEffectiveBalance())
          || validator.getEffectiveBalance().plus(half_increment.times(3)).less(balance)) {
        state.getValidators().update(index,
            v -> ValidatorRecord.Builder.fromRecord(v)
                .withEffectiveBalance(
                    UInt64s.min(
                        balance.minus(Gwei.castFrom(balance.modulo(getConstants().getEffectiveBalanceIncrement()))),
                        getConstants().getMaxEffectiveBalance()))
                .build());
      }
    }

    /* # Reset slashings
      state.slashings[next_epoch % EPOCHS_PER_SLASHINGS_VECTOR] = Gwei(0) */
    state.getSlashings().set(next_epoch.modulo(getConstants().getEpochsPerSlashingsVector()), Gwei.ZERO);

    /* # Set randao mix
      state.randao_mixes[next_epoch % EPOCHS_PER_HISTORICAL_VECTOR] = get_randao_mix(state, current_epoch */
    state.getRandaoMixes().set(next_epoch.modulo(getConstants().getEpochsPerHistoricalVector()),
        get_randao_mix(state, current_epoch));

    /* # Set historical root accumulator
    if next_epoch % (SLOTS_PER_HISTORICAL_ROOT // SLOTS_PER_EPOCH) == 0:
        historical_batch = HistoricalBatch(block_roots=state.block_roots, state_roots=state.state_roots)
        state.historical_roots.append(hash_tree_root(historical_batch)) */
    if (next_epoch.modulo(getConstants().getSlotsPerHistoricalRoot().dividedBy(getConstants().getSlotsPerEpoch()))
        .equals(EpochNumber.ZERO)) {
      HistoricalBatch historical_batch =
          new HistoricalBatch(state.getBlockRoots().vectorCopy(), state.getStateRoots().vectorCopy());
      state.getHistoricalRoots().add(hash_tree_root(historical_batch));
    }

    /* # Rotate current/previous epoch attestations
      state.previous_epoch_attestations = state.current_epoch_attestations
      state.current_epoch_attestations = [] */
    state.getPreviousEpochAttestations().replaceAll(state.getCurrentEpochAttestations().listCopy());
    state.getCurrentEpochAttestations().clear();
  }

  /*
    def process_epoch(state: BeaconState) -> None:
      process_justification_and_finalization(state)
      process_rewards_and_penalties(state)
      process_registry_updates(state)
      # @process_reveal_deadlines
      # @process_challenge_deadlines
      process_slashings(state)
      # @update_period_committee
      process_final_updates(state)
      # @after_process_final_updates
   */
  default void process_epoch(MutableBeaconState state) {
    process_justification_and_finalization(state);
    process_rewards_and_penalties(state);
    process_registry_updates(state);
    // @process_reveal_deadlines
    // @process_challenge_deadlines
    process_slashings(state);
    process_final_updates(state);
    // @after_process_final_updates
  }
}
