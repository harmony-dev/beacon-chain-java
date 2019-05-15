package org.ethereum.beacon.consensus.spec;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.HistoricalBatch;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

/**
 * Per epoch processing.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.1/specs/core/0_beacon-chain.md#per-epoch-processing">Per-epoch
 *     processing</a> in the spec.
 */
public interface EpochProcessing extends HelperFunction {

  /*
    def get_total_active_balance(state: BeaconState) -> Gwei:
      return get_total_balance(state, get_active_validator_indices(state, get_current_epoch(state)))
   */
  default Gwei get_total_active_balance(BeaconState state) {
    return get_total_balance(state,
        get_active_validator_indices(state, get_current_epoch(state)));
  }

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
    def get_matching_target_attestations(state: BeaconState, epoch: Epoch) -> List[PendingAttestation]:
      return [
          a for a in get_matching_source_attestations(state, epoch)
          if a.data.target_root == get_block_root(state, epoch)
      ]
   */
  default List<PendingAttestation> get_matching_target_attestations(BeaconState state, EpochNumber epoch) {
    return get_matching_source_attestations(state, epoch).stream()
        .filter(a -> a.getData().getTargetRoot().equals(get_block_root(state, epoch)))
        .collect(toList());
  }

  /*
    def get_matching_head_attestations(state: BeaconState, epoch: Epoch) -> List[PendingAttestation]:
      return [
          a for a in get_matching_source_attestations(state, epoch)
          if a.data.beacon_block_root == get_block_root_at_slot(state, get_attestation_slot(state, a))
      ]
   */
  default List<PendingAttestation> get_matching_head_attestations(BeaconState state, EpochNumber epoch) {
    return get_matching_source_attestations(state, epoch).stream()
        .filter(a -> a.getData().getBeaconBlockRoot().equals(
            get_block_root_at_slot(state, get_attestation_slot(state, a.getData()))))
        .collect(toList());
  }

  /*
    def get_unslashed_attesting_indices(state: BeaconState, attestations: List[PendingAttestation]) -> List[ValidatorIndex]:
      output = set()
      for a in attestations:
          output = output.union(get_attesting_indices(state, a.data, a.aggregation_bitfield))
      return sorted(filter(lambda index: not state.validator_registry[index].slashed, list(output)))
   */
  default List<ValidatorIndex> get_unslashed_attesting_indices(BeaconState state, List<PendingAttestation> attestations) {
    return attestations.stream()
        .flatMap(a -> get_attesting_indices(state, a.getData(), a.getAggregationBitfield()).stream())
        .distinct()
        .filter(i -> !state.getValidatorRegistry().get(i).getSlashed())
        .sorted()
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
    def get_crosslink_from_attestation_data(state: BeaconState, data: AttestationData) -> Crosslink:
      return Crosslink(
          epoch=min(data.target_epoch, state.current_crosslinks[data.shard].epoch + MAX_CROSSLINK_EPOCHS),
          previous_crosslink_root=data.previous_crosslink_root,
          crosslink_data_root=data.crosslink_data_root,
      )
   */
  default Crosslink get_crosslink_from_attestation_data(BeaconState state, AttestationData data) {
    return new Crosslink(
        UInt64s.min(
            data.getTargetEpoch(),
            state.getCurrentCrosslinks().get(data.getShard()).getEpoch()
                .plus(getConstants().getMaxCrosslinkEpochs())),
        data.getPreviousCrosslinkRoot(),
        data.getCrosslinkDataRoot()
    );
  }

  /*
    def get_attestations_for(crosslink: Crosslink) -> List[PendingAttestation]:
      return [a for a in shard_attestations if get_crosslink_from_attestation_data(state, a.data) == crosslink]
   */
  default List<PendingAttestation> get_attestations_for(
      BeaconState state, List<PendingAttestation> shard_attestations, Crosslink crosslink) {
    return shard_attestations.stream()
        .filter(a -> get_crosslink_from_attestation_data(state, a.getData()).equals(crosslink))
        .collect(toList());
  }

  /*
    def get_winning_crosslink_and_attesting_indices(state: BeaconState, epoch: Epoch, shard: Shard) -> Tuple[Crosslink, List[ValidatorIndex]]:
      shard_attestations = [a for a in get_matching_source_attestations(state, epoch) if a.data.shard == shard]
      shard_crosslinks = [get_crosslink_from_attestation_data(state, a.data) for a in shard_attestations]
      candidate_crosslinks = [
          c for c in shard_crosslinks
          if hash_tree_root(state.current_crosslinks[shard]) in (c.previous_crosslink_root, hash_tree_root(c))
      ]
      if len(candidate_crosslinks) == 0:
          return Crosslink(), []

      def get_attestations_for(crosslink: Crosslink) -> List[PendingAttestation]:
          return [a for a in shard_attestations if get_crosslink_from_attestation_data(state, a.data) == crosslink]
      # Winning crosslink has the crosslink data root with the most balance voting for it (ties broken lexicographically)
      winning_crosslink = max(candidate_crosslinks, key=lambda crosslink: (
          get_attesting_balance(state, get_attestations_for(crosslink)), crosslink.crosslink_data_root
      ))

      return winning_crosslink, get_unslashed_attesting_indices(state, get_attestations_for(winning_crosslink))
   */
  default Pair<Crosslink, List<ValidatorIndex>> get_winning_crosslink_and_attesting_indices(
      BeaconState state, EpochNumber epoch, ShardNumber shard) {
    List<PendingAttestation> shard_attestations = get_matching_source_attestations(state, epoch)
        .stream().filter(a -> a.getData().getShard().equals(shard)).collect(toList());
    List<Crosslink> shard_crosslinks = shard_attestations.stream()
        .map(a -> get_crosslink_from_attestation_data(state, a.getData())).collect(toList());
    List<Crosslink> candidate_crosslinks = shard_crosslinks.stream()
        .filter(c -> {
          Hash32 root = hash_tree_root(state.getCurrentCrosslinks().get(shard));
          return root.equals(c.getPreviousCrosslinkRoot()) || root.equals(hash_tree_root(c));
        }).collect(toList());
    if (candidate_crosslinks.isEmpty()) {
      return Pair.with(new Crosslink(getConstants().getGenesisEpoch(), Hash32.ZERO, Hash32.ZERO),
          Collections.emptyList());
    }

    Crosslink winning_crosslink = candidate_crosslinks.stream()
        .max((c1, c2) -> {
          Gwei b1 = get_attesting_balance(state, get_attestations_for(state, shard_attestations, c1));
          Gwei b2 = get_attesting_balance(state, get_attestations_for(state, shard_attestations, c2));
          if (b1.equals(b2)) {
            return c1.getCrosslinkDataRoot().toString().compareTo(c2.getCrosslinkDataRoot().toString());
          } else {
            return b1.compareTo(b2);
          }
        }).get();

    return Pair.with(winning_crosslink,
        get_unslashed_attesting_indices(state,
            get_attestations_for(state, shard_attestations, winning_crosslink)));
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
    EpochNumber old_previous_justified_epoch = state.getPreviousJustifiedEpoch();
    EpochNumber old_current_justified_epoch = state.getCurrentJustifiedEpoch();

    /* Process justifications
      state.previous_justified_epoch = state.current_justified_epoch
      state.previous_justified_root = state.current_justified_root
      state.justification_bitfield = (state.justification_bitfield << 1) % 2**64 */
    state.setPreviousJustifiedEpoch(state.getCurrentJustifiedEpoch());
    state.setPreviousJustifiedRoot(state.getCurrentJustifiedRoot());
    state.setJustificationBitfield(state.getJustificationBitfield().shl(1));

    /* previous_epoch_matching_target_balance = get_attesting_balance(state, get_matching_target_attestations(state, previous_epoch))
       if previous_epoch_matching_target_balance * 3 >= get_total_active_balance(state) * 2:
           state.current_justified_epoch = previous_epoch
           state.current_justified_root = get_block_root(state, state.current_justified_epoch)
           state.justification_bitfield |= (1 << 1) */
    Gwei previous_epoch_matching_target_balance =
        get_attesting_balance(state, get_matching_target_attestations(state, previous_epoch));
    if (previous_epoch_matching_target_balance.times(3)
        .greaterEqual(get_total_active_balance(state).times(2))) {
      state.setCurrentJustifiedEpoch(previous_epoch);
      state.setCurrentJustifiedRoot(get_block_root(state, state.getCurrentJustifiedEpoch()));
      state.setJustificationBitfield(state.getJustificationBitfield().or(2));
    }

    /* current_epoch_matching_target_balance = get_attesting_balance(state, get_matching_target_attestations(state, current_epoch))
       if current_epoch_matching_target_balance * 3 >= get_total_active_balance(state) * 2:
           state.current_justified_epoch = current_epoch
           state.current_justified_root = get_block_root(state, state.current_justified_epoch)
           state.justification_bitfield |= (1 << 0) */
    Gwei current_epoch_matching_target_balance =
        get_attesting_balance(state, get_matching_target_attestations(state, current_epoch));
    if (current_epoch_matching_target_balance.times(3)
        .greaterEqual(get_total_active_balance(state).times(2))) {
      state.setCurrentJustifiedEpoch(current_epoch);
      state.setCurrentJustifiedRoot(get_block_root(state, state.getCurrentJustifiedEpoch()));
      state.setJustificationBitfield(state.getJustificationBitfield().or(1));
    }

    /* Process finalizations
       bitfield = state.justification_bitfield */
    Bitfield64 bitfield = state.getJustificationBitfield();

    /* The 2nd/3rd/4th most recent epochs are justified, the 2nd using the 4th as source
       if (bitfield >> 1) % 8 == 0b111 and old_previous_justified_epoch == current_epoch - 3:
           state.finalized_epoch = old_previous_justified_epoch
           state.finalized_root = get_block_root(state, state.finalized_epoch) */
    if ((bitfield.getValue() >>> 1) % 8 == 0b111 && old_previous_justified_epoch.equals(current_epoch.minus(3))) {
      state.setFinalizedEpoch(old_current_justified_epoch);
      state.setFinalizedRoot(get_block_root(state, state.getFinalizedEpoch()));
    }

    /* The 2nd/3rd most recent epochs are justified, the 2nd using the 3rd as source
       if (bitfield >> 1) % 4 == 0b11 and old_previous_justified_epoch == current_epoch - 2:
           state.finalized_epoch = old_previous_justified_epoch
           state.finalized_root = get_block_root(state, state.finalized_epoch) */
    if ((bitfield.getValue() >>> 1) % 4 == 0b11 && old_previous_justified_epoch.equals(current_epoch.minus(2))) {
      state.setFinalizedEpoch(old_current_justified_epoch);
      state.setFinalizedRoot(get_block_root(state, state.getFinalizedEpoch()));
    }

    /* The 1st/2nd/3rd most recent epochs are justified, the 1st using the 3rd as source
       if (bitfield >> 0) % 8 == 0b111 and old_current_justified_epoch == current_epoch - 2:
           state.finalized_epoch = old_current_justified_epoch
           state.finalized_root = get_block_root(state, state.finalized_epoch) */
    if (bitfield.getValue() % 8 == 0b111 && old_previous_justified_epoch.equals(current_epoch.minus(2))) {
      state.setFinalizedEpoch(old_current_justified_epoch);
      state.setFinalizedRoot(get_block_root(state, state.getFinalizedEpoch()));
    }

    /* The 1st/2nd most recent epochs are justified, the 1st using the 2nd as source
       if (bitfield >> 0) % 4 == 0b11 and old_current_justified_epoch == current_epoch - 1:
           state.finalized_epoch = old_current_justified_epoch
           state.finalized_root = get_block_root(state, state.finalized_epoch) */
    if (bitfield.getValue() % 4 == 0b11 && old_previous_justified_epoch.equals(current_epoch.minus(1))) {
      state.setFinalizedEpoch(old_current_justified_epoch);
      state.setFinalizedRoot(get_block_root(state, state.getFinalizedEpoch()));
    }
  }

  /*
    def process_crosslinks(state: BeaconState) -> None:
      state.previous_crosslinks = [c for c in state.current_crosslinks]
      for epoch in (get_previous_epoch(state), get_current_epoch(state)):
          for offset in range(get_epoch_committee_count(state, epoch)):
              shard = (get_epoch_start_shard(state, epoch) + offset) % SHARD_COUNT
              crosslink_committee = get_crosslink_committee(state, epoch, shard)
              winning_crosslink, attesting_indices = get_winning_crosslink_and_attesting_indices(state, epoch, shard)
              if 3 * get_total_balance(state, attesting_indices) >= 2 * get_total_balance(state, crosslink_committee):
                  state.current_crosslinks[shard] = winning_crosslink
   */
  default void process_crosslinks(MutableBeaconState state) {
    state.getPreviousCrosslinks().clear();
    state.getPreviousCrosslinks().addAll(state.getCurrentCrosslinks().listCopy());

    for (EpochNumber epoch : get_previous_epoch(state).iterateTo(get_current_epoch(state).increment())) {
      for (UInt64 offset : UInt64s.iterate(UInt64.ZERO, get_epoch_committee_count(state, epoch))) {
        ShardNumber shard = get_epoch_start_shard(state, epoch)
            .plusModulo(offset, getConstants().getShardCount());
        List<ValidatorIndex> crosslink_committee = get_crosslink_committee(state, epoch, shard);
        Pair<Crosslink, List<ValidatorIndex>> winner =
            get_winning_crosslink_and_attesting_indices(state, epoch, shard);
        Crosslink winning_crosslink = winner.getValue0();
        List<ValidatorIndex> attesting_indices = winner.getValue1();
        if (get_total_balance(state, attesting_indices).times(3)
            .greaterEqual(get_total_balance(state, crosslink_committee).times(2))) {
          state.getCurrentCrosslinks().set(shard, winning_crosslink);
        }
      };
    }
  }

  /*
    def get_base_reward(state: BeaconState, index: ValidatorIndex) -> Gwei:
      adjusted_quotient = integer_squareroot(get_total_active_balance(state)) // BASE_REWARD_QUOTIENT
      if adjusted_quotient == 0:
          return 0
      return state.validator_registry[index].effective_balance // adjusted_quotient // BASE_REWARDS_PER_EPOCH
   */
  default Gwei get_base_reward(BeaconState state, ValidatorIndex index) {
    UInt64 adjusted_quotient = integer_squareroot(get_total_active_balance(state))
        .dividedBy(getConstants().getBaseRewardQuotient());
    if (adjusted_quotient.equals(UInt64.ZERO)) {
      return Gwei.ZERO;
    }
    return state.getValidatorRegistry().get(index).getEffectiveBalance()
        .dividedBy(adjusted_quotient).dividedBy(getConstants().getBaseRewardsPerEpoch());
  }

  /*
    def get_attestation_deltas(state: BeaconState) -> Tuple[List[Gwei], List[Gwei]]:
   */
  default Gwei[][] get_attestation_deltas(BeaconState state) {
    EpochNumber previous_epoch = get_previous_epoch(state);
    Gwei total_balance = get_total_active_balance(state);
    Gwei[] rewards = new Gwei[state.getValidatorRegistry().size().getIntValue()];
    Gwei[] penalties = new Gwei[state.getValidatorRegistry().size().getIntValue()];
    Arrays.fill(rewards, Gwei.ZERO);
    Arrays.fill(penalties, Gwei.ZERO);

    List<ValidatorIndex> eligible_validator_indices = new ArrayList<>();
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
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
          attesting_balance = get_attesting_balance(state, attestations)
          for index in eligible_validator_indices:
              if index in unslashed_attesting_indices:
                  rewards[index] += get_base_reward(state, index) * attesting_balance // total_balance
              else:
                  penalties[index] += get_base_reward(state, index) */
    for (List<PendingAttestation> attestations :
        Arrays.asList(matching_source_attestations, matching_target_attestations, matching_head_attestations)) {
      List<ValidatorIndex> unslashed_attesting_indices = get_unslashed_attesting_indices(state, attestations);
      Gwei attesting_balance = get_attesting_balance(state, attestations);
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

    /* Proposer and inclusion delay micro-rewards
      for index in get_unslashed_attesting_indices(state, matching_source_attestations):
        attestation = min([
            a for a in matching_source_attestations
            if index in get_attesting_indices(state, a.data, a.aggregation_bitfield)
        ], key=lambda a: a.inclusion_delay)
        rewards[attestation.proposer_index] += get_base_reward(state, index) // PROPOSER_REWARD_QUOTIENT
        rewards[index] += get_base_reward(state, index) * MIN_ATTESTATION_INCLUSION_DELAY // attestation.inclusion_delay */
    for (ValidatorIndex index : get_unslashed_attesting_indices(state, matching_source_attestations)) {
      PendingAttestation attestation =
          matching_source_attestations.stream()
              .filter(a -> get_attesting_indices(state, a.getData(), a.getAggregationBitfield()).contains(index))
              .min(Comparator.comparing(PendingAttestation::getInclusionDelay))
              .get();
      rewards[attestation.getProposerIndex().getIntValue()] = rewards[attestation.getProposerIndex().getIntValue()]
          .plus(get_base_reward(state, index).dividedBy(getConstants().getProposerRewardQuotient()));
      rewards[index.getIntValue()] = rewards[index.getIntValue()]
          .plus(get_base_reward(state, index)
              .times(getConstants().getMinAttestationInclusionDelay())
              .dividedBy(attestation.getInclusionDelay()));
    }

    /* Inactivity penalty
      finality_delay = previous_epoch - state.finalized_epoch
      if finality_delay > MIN_EPOCHS_TO_INACTIVITY_PENALTY:
          matching_target_attesting_indices = get_unslashed_attesting_indices(state, matching_target_attestations)
          for index in eligible_validator_indices:
              penalties[index] += BASE_REWARDS_PER_EPOCH * get_base_reward(state, index)
              if index not in matching_target_attesting_indices:
                  penalties[index] += state.validator_registry[index].effective_balance * finality_delay // INACTIVITY_PENALTY_QUOTIENT */
    EpochNumber finality_delay = previous_epoch.minus(state.getFinalizedEpoch());
    if (finality_delay.greater(getConstants().getMinEpochsToInactivityPenalty())) {
      List<ValidatorIndex> matching_target_attesting_indices = get_unslashed_attesting_indices(state, matching_target_attestations);
      for (ValidatorIndex index : eligible_validator_indices) {
        penalties[index.getIntValue()] = penalties[index.getIntValue()]
            .plus(get_base_reward(state, index).times(getConstants().getBaseRewardsPerEpoch()));
        if (!matching_target_attesting_indices.contains(index)) {
          penalties[index.getIntValue()] = penalties[index.getIntValue()]
              .plus(state.getValidatorRegistry().get(index).getEffectiveBalance()
                  .times(finality_delay).dividedBy(getConstants().getInactivityPenaltyQuotient()));
        }
      }
    }

    return new Gwei[][] { rewards, penalties };
  }

  /*
   def get_crosslink_deltas(state: BeaconState) -> Tuple[List[Gwei], List[Gwei]]:
    rewards = [0 for index in range(len(state.validator_registry))]
    penalties = [0 for index in range(len(state.validator_registry))]
    epoch = get_previous_epoch(state)
    for offset in range(get_epoch_committee_count(state, epoch)):
        shard = (get_epoch_start_shard(state, epoch) + offset) % SHARD_COUNT
        crosslink_committee = get_crosslink_committee(state, epoch, shard)
        winning_crosslink, attesting_indices = get_winning_crosslink_and_attesting_indices(state, epoch, shard)
        attesting_balance = get_total_balance(state, attesting_indices)
        committee_balance = get_total_balance(state, crosslink_committee)
        for index in crosslink_committee:
            base_reward = get_base_reward(state, index)
            if index in attesting_indices:
                rewards[index] += base_reward * attesting_balance // committee_balance
            else:
                penalties[index] += base_reward
    return rewards, penalties
  */
  default Gwei[][] get_crosslink_deltas(BeaconState state) {
    Gwei[] rewards = new Gwei[state.getValidatorRegistry().size().getIntValue()];
    Gwei[] penalties = new Gwei[state.getValidatorRegistry().size().getIntValue()];
    Arrays.fill(rewards, Gwei.ZERO);
    Arrays.fill(penalties, Gwei.ZERO);

    EpochNumber epoch = get_previous_epoch(state);
    for (UInt64 offset : UInt64s.iterate(UInt64.ZERO, get_epoch_committee_count(state, epoch))) {
      ShardNumber shard = get_epoch_start_shard(state, epoch)
          .plusModulo(offset, getConstants().getShardCount());
      List<ValidatorIndex> crosslink_committee = get_crosslink_committee(state, epoch, shard);
      Pair<Crosslink, List<ValidatorIndex>> winner =
          get_winning_crosslink_and_attesting_indices(state, epoch, shard);
      List<ValidatorIndex> attesting_indices = winner.getValue1();
      Gwei attesting_balance = get_total_balance(state, attesting_indices);
      Gwei committee_balance = get_total_balance(state, crosslink_committee);
      for (ValidatorIndex index : crosslink_committee) {
        Gwei base_reward = get_base_reward(state, index);
        if (attesting_indices.contains(index)) {
          rewards[index.getIntValue()] = rewards[index.getIntValue()]
              .plus(base_reward.times(attesting_balance).dividedBy(committee_balance));
        } else {
          penalties[index.getIntValue()] = penalties[index.getIntValue()]
              .plus(base_reward);
        }
      }
    }

    return new Gwei[][] { rewards, penalties };
  }

  /*
    def process_rewards_and_penalties(state: BeaconState) -> None:
      if get_current_epoch(state) == GENESIS_EPOCH:
          return

      rewards1, penalties1 = get_attestation_deltas(state)
      rewards2, penalties2 = get_crosslink_deltas(state)
      for i in range(len(state.validator_registry)):
          increase_balance(state, i, rewards1[i] + rewards2[i])
          decrease_balance(state, i, penalties1[i] + penalties2[i])
   */
  default void process_rewards_and_penalties(MutableBeaconState state) {
    if (get_current_epoch(state).equals(getConstants().getGenesisEpoch())) {
      return;
    }

    Gwei[][] deltas1 = get_attestation_deltas(state);
    Gwei[] rewards1 = deltas1[0], penalties1 = deltas1[1];
    Gwei[][] deltas2 = get_crosslink_deltas(state);
    Gwei[] rewards2 = deltas2[0], penalties2 = deltas2[1];
    for (ValidatorIndex i : state.getValidatorRegistry().size()) {
      increase_balance(state, i, rewards1[i.getIntValue()].plus(rewards2[i.getIntValue()]));
      decrease_balance(state, i, penalties1[i.getIntValue()].plus(penalties2[i.getIntValue()]));
    }
  }

  /*
    def process_registry_updates(state: BeaconState) -> None:
   */
  default List<ValidatorIndex> process_registry_updates(MutableBeaconState state) {
    /* Process activation eligibility and ejections
      for index, validator in enumerate(state.validator_registry):
          if validator.activation_eligibility_epoch == FAR_FUTURE_EPOCH and validator.effective_balance >= MAX_EFFECTIVE_BALANCE:
              validator.activation_eligibility_epoch = get_current_epoch(state)

          if is_active_validator(validator, get_current_epoch(state)) and validator.effective_balance <= EJECTION_BALANCE:
              initiate_validator_exit(state, index) */
    List<ValidatorIndex> ejected = new ArrayList<>();
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      if (validator.getActivationEligibilityEpoch().equals(getConstants().getFarFutureEpoch())
          && validator.getEffectiveBalance().greaterEqual(getConstants().getMaxEffectiveBalance())) {
        state.getValidatorRegistry().update(index,
            v -> ValidatorRecord.Builder.fromRecord(v)
                .withActivationEligibilityEpoch(get_current_epoch(state)).build());
      }

      if (is_active_validator(validator, get_current_epoch(state))
          && validator.getEffectiveBalance().lessEqual(getConstants().getEjectionBalance())) {
        initiate_validator_exit(state, index);
        ejected.add(index);
      }
    }

    /* Queue validators eligible for activation and not dequeued for activation prior to finalized epoch
      activation_queue = sorted([
          index for index, validator in enumerate(state.validator_registry) if
          validator.activation_eligibility_epoch != FAR_FUTURE_EPOCH and
          validator.activation_epoch >= get_delayed_activation_exit_epoch(state.finalized_epoch)
      ], key=lambda index: state.validator_registry[index].activation_eligibility_epoch) */
    List<Pair<ValidatorIndex, ValidatorRecord>> activation_queue = new ArrayList<>();
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord v = state.getValidatorRegistry().get(index);
      if (!v.getActivationEligibilityEpoch().equals(getConstants().getFarFutureEpoch())
          && v.getActivationEpoch().greaterEqual(get_delayed_activation_exit_epoch(state.getFinalizedEpoch()))) {
        activation_queue.add(Pair.with(index, v));
      }
    }
    activation_queue.sort(Comparator.comparing(p -> p.getValue1().getActivationEligibilityEpoch()));
    int limit = get_churn_limit(state).getIntValue();
    List<Pair<ValidatorIndex, ValidatorRecord>> limited_activation_queue =
        activation_queue.size() > limit ? activation_queue.subList(0, limit) : activation_queue;

    /* Dequeued validators for activation up to churn limit (without resetting activation epoch)
      for index in activation_queue[:get_churn_limit(state)]:
          if validator.activation_epoch == FAR_FUTURE_EPOCH:
              validator.activation_epoch = get_delayed_activation_exit_epoch(get_current_epoch(state)) */
    for (Pair<ValidatorIndex, ValidatorRecord> p : limited_activation_queue) {
      if (p.getValue1().getActivationEpoch().equals(getConstants().getFarFutureEpoch())) {
        state.getValidatorRegistry().update(p.getValue0(),
            v -> ValidatorRecord.Builder.fromRecord(v)
                .withActivationEpoch(get_delayed_activation_exit_epoch(get_current_epoch(state)))
                .build());
      }
    }

    return ejected;
  }

  /*
    def process_slashings(state: BeaconState) -> None:
   */
  default void process_slashings(MutableBeaconState state) {
    /* current_epoch = get_current_epoch(state)
       active_validator_indices = get_active_validator_indices(state, current_epoch)
       total_balance = get_total_balance(state, active_validator_indices) */
    EpochNumber current_epoch = get_current_epoch(state);
    List<ValidatorIndex> active_validator_indices = get_active_validator_indices(state, current_epoch);
    Gwei total_balance = get_total_balance(state, active_validator_indices);

    /* Compute `total_penalties`
      total_at_start = state.latest_slashed_balances[(current_epoch + 1) % LATEST_SLASHED_EXIT_LENGTH]
      total_at_end = state.latest_slashed_balances[current_epoch % LATEST_SLASHED_EXIT_LENGTH]
      total_penalties = total_at_end - total_at_start */
    Gwei total_at_start = state.getLatestSlashedBalances().get(
        current_epoch.increment().modulo(getConstants().getLatestSlashedExitLength()));
    Gwei total_at_end = state.getLatestSlashedBalances().get(
        current_epoch.modulo(getConstants().getLatestSlashedExitLength()));
    Gwei total_penalties = total_at_end.minus(total_at_start);

    /* for index, validator in enumerate(state.validator_registry):
        if validator.slashed and current_epoch == validator.withdrawable_epoch - LATEST_SLASHED_EXIT_LENGTH // 2:
            penalty = max(
                validator.effective_balance * min(total_penalties * 3, total_balance) // total_balance,
                validator.effective_balance // MIN_SLASHING_PENALTY_QUOTIENT
            )
            decrease_balance(state, index, penalty) */
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      if (validator.getSlashed()
          && current_epoch.equals(
              validator.getWithdrawableEpoch().minus(getConstants().getLatestSlashedExitLength().half()))) {
        Gwei total_penalty_multiplier = UInt64s.min(total_penalties.times(3), total_balance);
        Gwei penalty = UInt64s.max(
            validator.getEffectiveBalance().times(total_penalty_multiplier).dividedBy(total_balance),
            validator.getEffectiveBalance().dividedBy(getConstants().getMinSlashingPenaltyQuotient())
        );
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
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      Gwei balance = state.getBalances().get(index);
      if (balance.less(validator.getEffectiveBalance())
          || validator.getEffectiveBalance().plus(half_increment.times(3)).less(balance)) {
        state.getValidatorRegistry().update(index,
            v -> ValidatorRecord.Builder.fromRecord(v)
                .withEffectiveBalance(
                    UInt64s.min(
                        balance.minus(Gwei.castFrom(balance.modulo(getConstants().getEffectiveBalanceIncrement()))),
                        getConstants().getMaxEffectiveBalance()))
                .build());
      }
    }

    /* Update start shard
      state.latest_start_shard = (state.latest_start_shard + get_shard_delta(state, current_epoch)) % SHARD_COUNT */
    state.setLatestStartShard(state.getLatestStartShard()
        .plusModulo(get_shard_delta(state, current_epoch), getConstants().getShardCount()));

    /* Set active index root
      index_root_position = (next_epoch + ACTIVATION_EXIT_DELAY) % LATEST_ACTIVE_INDEX_ROOTS_LENGTH
      state.latest_active_index_roots[index_root_position] = hash_tree_root(
          get_active_validator_indices(state, next_epoch + ACTIVATION_EXIT_DELAY)
      ) */
    EpochNumber index_root_position = next_epoch.plusModulo(getConstants().getActivationExitDelay(),
        getConstants().getLatestActiveIndexRootsLength());
    state.getLatestActiveIndexRoots().set(index_root_position,
        hash_tree_root(
            get_active_validator_indices(state, next_epoch.plus(getConstants().getActivationExitDelay()))));

    /* Set total slashed balances
      state.latest_slashed_balances[next_epoch % LATEST_SLASHED_EXIT_LENGTH] = (
          state.latest_slashed_balances[current_epoch % LATEST_SLASHED_EXIT_LENGTH]
      ) */
    state.getLatestSlashedBalances().set(next_epoch.modulo(getConstants().getLatestSlashedExitLength()),
        state.getLatestSlashedBalances().get(current_epoch.modulo(getConstants().getLatestSlashedExitLength())));

    /* Set randao mix
      state.latest_randao_mixes[next_epoch % LATEST_RANDAO_MIXES_LENGTH] = get_randao_mix(state, current_epoch) */
    state.getLatestRandaoMixes().set(next_epoch.modulo(getConstants().getLatestRandaoMixesLength()),
        get_randao_mix(state, current_epoch));

    /* Set historical root accumulator
    if next_epoch % (SLOTS_PER_HISTORICAL_ROOT // SLOTS_PER_EPOCH) == 0:
        historical_batch = HistoricalBatch(
            block_roots=state.latest_block_roots,
            state_roots=state.latest_state_roots,
        )
        state.historical_roots.append(hash_tree_root(historical_batch)) */
    if (next_epoch.modulo(getConstants().getSlotsPerHistoricalRoot().dividedBy(getConstants().getSlotsPerEpoch()))
        .equals(EpochNumber.ZERO)) {
      HistoricalBatch historical_batch =
          new HistoricalBatch(state.getLatestBlockRoots().vectorCopy(), state.getLatestStateRoots().vectorCopy());
      state.getHistoricalRoots().add(hash_tree_root(historical_batch));
    }

    /* Rotate current/previous epoch attestations
      state.previous_epoch_attestations = state.current_epoch_attestations
      state.current_epoch_attestations = [] */
    state.getPreviousEpochAttestations().clear();
    state.getPreviousEpochAttestations().addAll(state.getCurrentEpochAttestations().listCopy());
    state.getCurrentEpochAttestations().clear();
  }
}
