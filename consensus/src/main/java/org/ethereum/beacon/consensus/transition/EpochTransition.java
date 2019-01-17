package org.ethereum.beacon.consensus.transition;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.CrosslinkRecord;
import org.ethereum.beacon.core.state.DepositRootVote;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.types.Ether;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public class EpochTransition implements StateTransition<BeaconStateEx> {

  private final ChainSpec spec;
  private final SpecHelpers specHelpers;

  public EpochTransition(SpecHelpers specHelpers) {
    this.specHelpers = specHelpers;
    this.spec = specHelpers.getChainSpec();
  }

  @Override
  public BeaconStateEx apply(BeaconBlock block, BeaconStateEx stateEx) {
    MutableBeaconState state = stateEx.getCanonicalState().createMutableCopy();

    /*
     Helpers: All validators:
    */

    // Let active_validator_indices =
    //      get_active_validator_indices(state.validator_registry, state.slot).
    List<UInt24> active_validator_indices = specHelpers.get_active_validator_indices(
        state.getValidatorRegistry(), state.getSlot());

    // Let total_balance = sum([get_effective_balance(state, i) for i in active_validator_indices])
    UInt64 total_balance = active_validator_indices.stream()
        .map(i -> specHelpers.get_effective_balance(state, i))
        .reduce(UInt64::plus)
        .orElse(UInt64.ZERO);

    /*
     Helpers: Validators attesting during the current epoch:
    */

    // Let current_epoch_attestations =
    //  [a for a in state.latest_attestations if state.slot - EPOCH_LENGTH <= a.data.slot <
    // state.slot].
    // (Note: this is the set of attestations of slots in the
    // epoch state.slot-EPOCH_LENGTH...state.slot-1, not attestations that got included in the
    // chain during the epoch state.slot-EPOCH_LENGTH...state.slot-1.)
    List<PendingAttestationRecord> current_epoch_attestations =
        state.getLatestAttestations().stream()
            .filter(a ->
                state.getSlot().minus(spec.getEpochLength())
                    .compareTo(a.getData().getSlot()) <= 0
                    && a.getData().getSlot().compareTo(state.getSlot()) < 0)
            .collect(Collectors.toList());

    // Validators justifying the epoch boundary block at the start of the current epoch:

    // Let current_epoch_boundary_attestations = [a for a in current_epoch_attestations
    //      if a.data.epoch_boundary_root == get_block_root(state, state.slot-EPOCH_LENGTH)
    //      and a.justified_slot == state.justified_slot].
    // FIXME: a.justified_slot => a.data.justified_slot
    List<PendingAttestationRecord> current_epoch_boundary_attestations =
        current_epoch_attestations.stream().filter(a ->
            a.getData().getEpochBoundaryRoot().equals(
                specHelpers.get_block_root(state, state.getSlot().minus(spec.getEpochLength())))
                && a.getData().getJustifiedSlot().equals(state.getJustifiedSlot())
        ).collect(Collectors.toList());

    // Let current_epoch_boundary_attester_indices be the union of the validator index sets
    // given by [get_attestation_participants(state, a.data, a.participation_bitfield)
    //    for a in current_epoch_boundary_attestations].
    Set<UInt24> current_epoch_boundary_attester_indices = current_epoch_boundary_attestations
        .stream()
        .flatMap(a ->
            specHelpers
                .get_attestation_participants(state, a.getData(), a.getParticipationBitfield())
                .stream())
        .collect(Collectors.toSet());

    // Let current_epoch_boundary_attesting_balance =
    //    sum([get_effective_balance(state, i) for i in current_epoch_boundary_attester_indices]).
    UInt64 current_epoch_boundary_attesting_balance = current_epoch_boundary_attester_indices
        .stream()
        .map(i -> specHelpers.get_effective_balance(state, i))
        .reduce(UInt64::plus)
        .orElse(UInt64.ZERO);

    /*
     Helpers: Validators attesting during the previous epoch:
    */

    // Validators that made an attestation during the previous epoch:

    // Let previous_epoch_attestations = [a for a in state.latest_attestations
    //    if state.slot - 2 * EPOCH_LENGTH <= a.slot < state.slot - EPOCH_LENGTH].
    // FIXME a.slot => a.data.slot
    List<PendingAttestationRecord> previous_epoch_attestations = state.getLatestAttestations()
        .stream()
        .filter(a ->
            state.getSlot().minus(spec.getEpochLength().times(2))
                .compareTo(a.getData().getSlot()) <= 0
                && a.getData().getSlot()
                .compareTo(state.getSlot().minus(spec.getEpochLength())) < 0)
        .collect(Collectors.toList());

    // Let previous_epoch_attester_indices be the union of the validator index sets given by
    //    [get_attestation_participants(state, a.data, a.participation_bitfield)
    //        for a in previous_epoch_attestations]
    Set<UInt24> previous_epoch_attester_indices = previous_epoch_attestations
        .stream()
        .flatMap(a -> specHelpers
            .get_attestation_participants(state, a.getData(), a.getParticipationBitfield())
            .stream())
        .collect(Collectors.toSet());

    // Validators targeting the previous justified slot:

    // Let previous_epoch_justified_attestations =
    //    [a for a in current_epoch_attestations + previous_epoch_attestations
    //        if a.justified_slot == state.previous_justified_slot].
    List<PendingAttestationRecord> previous_epoch_justified_attestations = Stream
        .concat(current_epoch_attestations.stream(), previous_epoch_attestations.stream())
        .filter(a -> a.getData().getJustifiedSlot().equals(state.getPreviousJustifiedSlot()))
        .collect(Collectors.toList());

    // Let previous_epoch_justified_attester_indices be the union of the validator index sets given by
    // [get_attestation_participants(state, a.data, a.participation_bitfield)
    //      for a in previous_epoch_justified_attestations].
    Set<UInt24> previous_epoch_justified_attester_indices = previous_epoch_justified_attestations
        .stream()
        .flatMap(a -> specHelpers.get_attestation_participants(
            state, a.getData(), a.getParticipationBitfield()).stream())
        .collect(Collectors.toSet());

    // Let previous_epoch_justified_attesting_balance = sum([get_effective_balance(state, i)
    //    for i in previous_epoch_justified_attester_indices]).
    UInt64 previous_epoch_justified_attesting_balance = previous_epoch_justified_attester_indices
        .stream()
        .map(i -> specHelpers.get_effective_balance(state, i))
        .reduce(UInt64::plus)
        .orElse(UInt64.ZERO);

    // Validators justifying the epoch boundary block at the start of the previous epoch:

    // Let previous_epoch_boundary_attestations = [a for a in previous_epoch_justified_attestations
    //    if a.epoch_boundary_root == get_block_root(state, state.slot - 2 * EPOCH_LENGTH)].
    // FIXME: a.epoch_boundary_root => a.data.epoch_boundary_root
    List<PendingAttestationRecord> previous_epoch_boundary_attestations =
        previous_epoch_justified_attestations.stream()
            .filter(a -> a.getData().getEpochBoundaryRoot()
                .equals(specHelpers.get_block_root(state,
                    state.getSlot().minus(spec.getEpochLength().times(2)))))
            .collect(Collectors.toList());

    // Let previous_epoch_boundary_attester_indices be the union of the validator index sets
    // given by [get_attestation_participants(state, a.data, a.participation_bitfield)
    //    for a in previous_epoch_boundary_attestations].
    Set<UInt24> previous_epoch_boundary_attester_indices = previous_epoch_boundary_attestations
        .stream()
        .flatMap(a -> specHelpers.get_attestation_participants(
            state, a.getData(), a.getParticipationBitfield()).stream())
        .collect(Collectors.toSet());

    // Let previous_epoch_boundary_attesting_balance = sum([get_effective_balance(state, i)
    //    for i in previous_epoch_boundary_attester_indices]).
    UInt64 previous_epoch_boundary_attesting_balance = previous_epoch_boundary_attester_indices
        .stream()
        .map(i -> specHelpers.get_effective_balance(state, i))
        .reduce(UInt64::plus)
        .orElse(UInt64.ZERO);

    // Validators attesting to the expected beacon chain head during the previous epoch:

    // Let previous_epoch_head_attestations = [a for a in previous_epoch_attestations
    //    if a.beacon_block_root == get_block_root(state, a.slot)].
    List<PendingAttestationRecord> previous_epoch_head_attestations = previous_epoch_attestations
        .stream()
        .filter(a -> a.getData().getBeaconBlockRoot()
            .equals(specHelpers.get_block_root(state, a.getData().getSlot())))
        .collect(Collectors.toList());

    // Let previous_epoch_head_attester_indices be the union of the validator index sets given by
    // [get_attestation_participants(state, a.data, a.participation_bitfield)
    //    for a in previous_epoch_head_attestations].
    Set<UInt24> previous_epoch_head_attester_indices = previous_epoch_head_attestations.stream()
        .flatMap(a -> specHelpers.get_attestation_participants(
            state, a.getData(), a.getParticipationBitfield()).stream())
        .collect(Collectors.toSet());

    // Let previous_epoch_head_attesting_balance = sum([get_effective_balance(state, i)
    //    for i in previous_epoch_head_attester_indices]).
    UInt64 previous_epoch_head_attesting_balance = previous_epoch_head_attester_indices.stream()
        .map(i -> specHelpers.get_effective_balance(state, i))
        .reduce(UInt64::plus)
        .orElse(UInt64.ZERO);

    Map<Pair<List<UInt24>, Hash32>, Set<UInt24>> attesting_validator_indices = new HashMap<>();
    Map<List<UInt24>, Pair<UInt64, Hash32>> winning_root_tmp = new HashMap<>();

    // For every slot in range(state.slot - 2 * EPOCH_LENGTH, state.slot),
    // let shard_committee_at_slot = get_shard_committees_at_slot(slot).
    // For every (shard_committee, shard) in shard_committee_at_slot, compute:
    for (UInt64 slot = state.getSlot().minus(spec.getEpochLength().times(2));
        slot.compareTo(state.getSlot()) < 0; slot = slot.increment()) {
      List<Pair<UInt24[], UInt64>> shard_committees_at_slot = specHelpers
          .get_shard_committees_at_slot(state, slot);
      for (Pair<UInt24[], UInt64> s : shard_committees_at_slot) {
        UInt24[] shard_committee = s.getValue0();
        UInt64 shard = s.getValue1();
        List<UInt24> shard_committee_list = Arrays.asList(shard_committee);

        // Let shard_block_root be state.latest_crosslinks[shard].shard_block_root
        Hash32 shard_block_root = state.getLatestCrosslinks()
            .get(shard.getIntValue()).getShardBlockRoot();

        // Let attesting_validator_indices(shard_committee, shard_block_root)
        // be the union of the validator index sets given by
        // [get_attestation_participants(state, a.data, a.participation_bitfield)
        //    for a in current_epoch_attestations + previous_epoch_attestations
        //    if a.shard == shard and a.shard_block_root == shard_block_root]
        // FIXME a.shard => a.data.shard
        Set<UInt24> attesting_validator_indices_tmp = Stream
            .concat(current_epoch_attestations.stream(), previous_epoch_attestations.stream())
            .filter(a -> a.getData().getShard().equals(shard)
                && a.getData().getShardBlockRoot().equals(shard_block_root))
            .flatMap(a -> specHelpers.get_attestation_participants(
                state, a.getData(), a.getParticipationBitfield()).stream())
            .collect(Collectors.toSet());

        attesting_validator_indices.put(
            Pair.with(shard_committee_list, shard_block_root),
            attesting_validator_indices_tmp);

        // Let winning_root(shard_committee) be equal to the value of shard_block_root
        // such that sum([get_effective_balance(state, i)
        // for i in attesting_validator_indices(shard_committee, shard_block_root)])
        // is maximized (ties broken by favoring lower shard_block_root values).

        // TODO not sure this is correct implementation
        UInt64 sum = attesting_validator_indices_tmp.stream()
            .map(i -> specHelpers.get_effective_balance(state, i))
            .reduce(UInt64::plus)
            .orElse(UInt64.ZERO);
        winning_root_tmp.compute(shard_committee_list, (k, v) ->
            v == null || sum.compareTo(v.getValue0()) > 0 ?
                Pair.with(sum, shard_block_root) : v
        );
      }
    }
    Map<List<UInt24>, Hash32> winning_root = winning_root_tmp.entrySet().stream()
        .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getValue1()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Let attesting_validators(shard_committee) be equal to
    // attesting_validator_indices(shard_committee, winning_root(shard_committee)) for convenience.
    Function<List<UInt24>, Set<UInt24>> attesting_validators = shard_committee ->
        attesting_validator_indices.get(
            Pair.with(shard_committee, winning_root.get(shard_committee)));

    // Let total_attesting_balance(shard_committee) =
    // sum([get_effective_balance(state, i) for i in attesting_validators(shard_committee)])
    Function<List<UInt24>, UInt64> total_attesting_balance = shard_committee ->
        attesting_validators.apply(shard_committee).stream()
            .map(i -> specHelpers.get_effective_balance(state, i))
            .reduce(UInt64::plus)
            .orElse(UInt64.ZERO);

    // Let total_balance(shard_committee) = sum([get_effective_balance(state, i)
    //    for i in shard_committee]).
    // FIXME total_balance name conflict
    Function<List<UInt24>, UInt64> total_balance_func = shard_committee ->
        shard_committee.stream()
            .map(i -> specHelpers.get_effective_balance(state, i))
            .reduce(UInt64::plus)
            .orElse(UInt64.ZERO);

    // Define the following helpers to process attestation inclusion rewards
    //  and inclusion distance reward/penalty

    // For every attestation a in previous_epoch_attestations:
    //    Let inclusion_slot(state, index) = a.slot_included for the attestation a
    //      where index is in get_attestation_participants(state, a.data, a.participation_bitfield).
    //    Let inclusion_distance(state, index) = a.slot_included - a.data.slot
    //      where a is the above attestation. FIXME fuzzy spec
    Map<UInt24, UInt64> inclusion_slot = new HashMap<>();
    Map<UInt24, UInt64> inclusion_distance = new HashMap<>();
    for (PendingAttestationRecord a : previous_epoch_attestations) {
      List<UInt24> attestation_participants = specHelpers
          .get_attestation_participants(state, a.getData(), a.getParticipationBitfield());
      for (UInt24 participant : attestation_participants) {
        inclusion_slot.put(participant, a.getSlotIncluded());
        inclusion_distance.put(participant, a.getSlotIncluded().minus(a.getData().getSlot()));
      }
    }

    /*
     Deposit roots

     Set state.latest_deposit_root = deposit_root_vote.deposit_root
         if deposit_root_vote.vote_count * 2 > DEPOSIT_ROOT_VOTING_PERIOD
         for some deposit_root_vote in state.deposit_root_votes.
     Set state.deposit_root_votes = [].
    */
    for (DepositRootVote deposit_root_vote : state.getDepositRootVotes()) {
      if (deposit_root_vote.getVoteCount().times(2)
          .compareTo(spec.getDepositRootVotingPeriod()) > 0) {
        state.withLatestDepositRoot(deposit_root_vote.getDepositRoot());
        break;
      }
    }
    state.withDepositRootVotes(Collections.emptyList());

    /*
     Justification
    */

    // Set state.previous_justified_slot = state.justified_slot.
    state.withPreviousJustifiedSlot(state.getJustifiedSlot());

    // Set state.justification_bitfield = (state.justification_bitfield * 2) % 2**64.
    state.withJustificationBitfield(state.getJustificationBitfield().shl(1));

    // Set state.justification_bitfield |= 2 and
    //    state.justified_slot = state.slot - 2 * EPOCH_LENGTH
    // if 3 * previous_epoch_boundary_attesting_balance >= 2 * total_balance.
    if (previous_epoch_boundary_attesting_balance.times(3)
        .compareTo(total_balance.times(2)) >= 0) {
      state.withJustificationBitfield(state.getJustificationBitfield().or(UInt64.valueOf(2)));
      state.withJustifiedSlot(state.getSlot().minus(spec.getEpochLength().times(2)));
    }

    // Set state.justification_bitfield |= 1 and
    //    state.justified_slot = state.slot - 1 * EPOCH_LENGTH
    // if 3 * current_epoch_boundary_attesting_balance >= 2 * total_balance.
    if (current_epoch_boundary_attesting_balance.times(3)
        .compareTo(total_balance.times(2)) >= 0) {
      state.withJustificationBitfield(state.getJustificationBitfield().or(UInt64.valueOf(1)));
      state.withJustifiedSlot(state.getSlot().minus(spec.getEpochLength().times(1)));
    }

    // Set state.finalized_slot = state.previous_justified_slot if any of the following are true:
    // state.previous_justified_slot == state.slot - 2 * EPOCH_LENGTH
    //     and state.justification_bitfield % 4 == 3
    // state.previous_justified_slot == state.slot - 3 * EPOCH_LENGTH
    //     and state.justification_bitfield % 8 == 7
    // state.previous_justified_slot == state.slot - 4 * EPOCH_LENGTH
    //     and state.justification_bitfield % 16 in (15, 14)
    if (state.getPreviousJustifiedSlot()
        .equals(state.getSlot().minus(spec.getEpochLength().times(2)))
        && state.getJustificationBitfield().modulo(4).getValue() == 3
        || state.getPreviousJustifiedSlot()
        .equals(state.getSlot().minus(spec.getEpochLength().times(3)))
        && state.getJustificationBitfield().modulo(8).getValue() == 7
        || state.getPreviousJustifiedSlot()
        .equals(state.getSlot().minus(spec.getEpochLength().times(4)))
        && (state.getJustificationBitfield().modulo(16).getValue() == 15
        || state.getJustificationBitfield().modulo(16).getValue() == 14)) {
      state.withFinalizedSlot(state.getPreviousJustifiedSlot());
    }

    /*
     For every slot in range(state.slot - 2 * EPOCH_LENGTH, state.slot),
     let shard_committee_at_slot = get_shard_committees_at_slot(slot).
     For every (shard_committee, shard) in shard_committee_at_slot, compute:

       Set state.latest_crosslinks[shard] = CrosslinkRecord(
           slot=state.slot,
           shard_block_root=winning_root(shard_committee))
       if 3 * total_attesting_balance(shard_committee) >= 2 * total_balance(shard_committee).
    */

    List<CrosslinkRecord> newLatestCrosslinks = new ArrayList<>(state.getLatestCrosslinks());
    LongStream.range(0, spec.getEpochLength().times(2).getValue())
        .mapToObj(i -> state.getSlot().minus(spec.getEpochLength().times(2)).plus(i))
        .flatMap(slot -> specHelpers.get_shard_committees_at_slot(state, slot).stream())
        .filter(shardCom -> {
          List<UInt24> shard_committee = Arrays.asList(shardCom.getValue0());
          return total_attesting_balance.apply(shard_committee).times(3)
              .compareTo(total_balance_func.apply(shard_committee).times(3)) >= 0;
        })
        .forEachOrdered(shard -> {
          newLatestCrosslinks.set(shard.getValue1().getIntValue(),
              new CrosslinkRecord(
                  state.getSlot(),
                  winning_root.get(Arrays.asList(shard.getValue0()))));
        });
    state.withLatestCrosslinks(newLatestCrosslinks);

    /*
     Rewards and penalties
     First, we define some additional helpers:
     */

    //     Let base_reward_quotient = BASE_REWARD_QUOTIENT *
    //        integer_squareroot(total_balance // GWEI_PER_ETH)
    UInt64 base_reward_quotient = spec.getBaseRewardQuotient()
        .times(specHelpers.integer_squareroot(Ether.ofGWeis(total_balance).getEthers()));

    // Let base_reward(state, index) = get_effective_balance(state, index) //
    //    base_reward_quotient // 5 for any validator with the given index.
    Function<UInt24, UInt64> base_reward = index ->
        specHelpers.get_effective_balance(state, index)
            .dividedBy(base_reward_quotient)
            .dividedBy(5);

    // Let inactivity_penalty(state, index, epochs_since_finality) = base_reward(state, index) +
    // get_effective_balance(state, index) * epochs_since_finality //
    // INACTIVITY_PENALTY_QUOTIENT // 2 for any validator with the given index.

    BiFunction<UInt24, UInt64, UInt64> inactivity_penalty = (index, epochs_since_finality) ->
        base_reward.apply(index).plus(
            specHelpers.get_effective_balance(state, index)
                .times(epochs_since_finality)
                .dividedBy(spec.getInactivityPenaltyQuotient())
                .dividedBy(2));

    /*
     Justification and finalization
    */

    //    Let epochs_since_finality = (state.slot - state.finalized_slot) // EPOCH_LENGTH.
    UInt64 epochs_since_finality = state.getSlot()
        .minus(state.getFinalizedSlot())
        .dividedBy(spec.getEpochLength());

    if (epochs_since_finality.compareTo(UInt64.valueOf(4)) <= 0) {
      // Case 1: epochs_since_finality <= 4:

      //  Expected FFG source:

      //  Any validator index in previous_epoch_justified_attester_indices gains
      //    base_reward(state, index) * previous_epoch_justified_attesting_balance // total_balance.
      for (UInt24 index : previous_epoch_justified_attester_indices) {
        state.withValidatorBalance(index, balance ->
            balance
                .plus(base_reward.apply(index)
                    .times(previous_epoch_justified_attesting_balance)
                    .dividedBy(total_balance)));
      }
      //  Any active validator v not in previous_epoch_justified_attester_indices loses
      //    base_reward(state, index).
      //  FIXME 'active validator' - not exact meaning
      for (UInt24 index : active_validator_indices) {
        if (!previous_epoch_justified_attester_indices.contains(index)) {
          state.withValidatorBalance(index, balance ->
              balance.minus(base_reward.apply(index)));
        }
      }

      //  Expected FFG target:

      //  Any validator index in previous_epoch_boundary_attester_indices gains
      //    base_reward(state, index) * previous_epoch_boundary_attesting_balance // total_balance.
      for (UInt24 index : previous_epoch_boundary_attester_indices) {
        state.withValidatorBalance(index, balance ->
            balance
                .plus(base_reward.apply(index)
                    .times(previous_epoch_boundary_attesting_balance)
                    .dividedBy(total_balance)));
      }
      //  Any active validator index not in previous_epoch_boundary_attester_indices loses
      //    base_reward(state, index).
      for (UInt24 index : active_validator_indices) {
        if (!previous_epoch_boundary_attester_indices.contains(index)) {
          state.withValidatorBalance(index, balance ->
              balance.minus(base_reward.apply(index)));
        }
      }

      //  Expected beacon chain head:

      //  Any validator index in previous_epoch_head_attester_indices gains
      //    base_reward(state, index) * previous_epoch_head_attesting_balance // total_balance).
      for (UInt24 index : previous_epoch_head_attester_indices) {
        state.withValidatorBalance(index, balance ->
            balance
                .plus(base_reward.apply(index)
                    .times(previous_epoch_head_attesting_balance)
                    .dividedBy(total_balance)));
      }
      //  Any active validator index not in previous_epoch_head_attester_indices loses
      //    base_reward(state, index).
      for (UInt24 index : active_validator_indices) {
        if (!previous_epoch_head_attester_indices.contains(index)) {
          state.withValidatorBalance(index, balance ->
              balance.minus(base_reward.apply(index)));
        }
      }

      //  Inclusion distance:

      // Any validator index in previous_epoch_attester_indices gains
      //    base_reward(state, index) * MIN_ATTESTATION_INCLUSION_DELAY //
      //        inclusion_distance(state, index)
      for (UInt24 index : previous_epoch_attester_indices) {
        state.withValidatorBalance(index, balance ->
            balance
                .plus(base_reward.apply(index)
                    .times(spec.getMinAttestationInclusionDelay())
                    .dividedBy(inclusion_distance.get(index))));
      }
    } else {
      // Case 2: epochs_since_finality > 4:

      //  Any active validator index not in previous_epoch_justified_attester_indices, loses
      //      inactivity_penalty(state, index, epochs_since_finality).
      for (UInt24 index : active_validator_indices) {
        if (!previous_epoch_justified_attester_indices.contains(index)) {
          state.withValidatorBalance(index, balance ->
              balance.minus(inactivity_penalty.apply(index, epochs_since_finality)));
        }
      }
      //  Any active validator index not in previous_epoch_boundary_attester_indices, loses
      //      inactivity_penalty(state, index, epochs_since_finality).
      for (UInt24 index : active_validator_indices) {
        if (!previous_epoch_boundary_attester_indices.contains(index)) {
          state.withValidatorBalance(index, balance ->
              balance.minus(inactivity_penalty.apply(index, epochs_since_finality)));
        }
      }
      //  Any active validator index not in previous_epoch_head_attester_indices, loses
      //      base_reward(state, index).
      for (UInt24 index : active_validator_indices) {
        if (!previous_epoch_head_attester_indices.contains(index)) {
          state.withValidatorBalance(index, balance ->
              balance.minus(base_reward.apply(index)));
        }
      }
      //  Any active_validator index with validator.penalized_slot <= state.slot, loses
      //      2 * inactivity_penalty(state, index, epochs_since_finality) + base_reward(state, index).
      for (UInt24 index : active_validator_indices) {
        ValidatorRecord validator = state.getValidatorRegistry().get(index.getValue());
        if (validator.getPenalizedSlot().compareTo(state.getSlot()) <= 0) {
          state.withValidatorBalance(index, balance ->
              balance.minus(
                  inactivity_penalty.apply(index, epochs_since_finality))
                      .times(2)
                      .plus(base_reward.apply(index))
          );
        }
      }
      //  Any validator index in previous_epoch_attester_indices loses
      //    base_reward(state, index) - base_reward(state, index) *
      //        MIN_ATTESTATION_INCLUSION_DELAY // inclusion_distance(state, index)
      for (UInt24 index : previous_epoch_attester_indices) {
        state.withValidatorBalance(index, balance -> balance.minus(
            base_reward.apply(index).minus(
                base_reward.apply(index)
                .times(spec.getMinAttestationInclusionDelay())
                .dividedBy(inclusion_distance.get(index))
            )
        ));
      }
    }

    /*
    Attestation inclusion
    */

    // For each index in previous_epoch_attester_indices, we determine the proposer
    //    proposer_index = get_beacon_proposer_index(state, inclusion_slot(state, index))
    //    and set state.validator_balances[proposer_index] +=
    //    base_reward(state, index) // INCLUDER_REWARD_QUOTIENT
    for (UInt24 index : previous_epoch_attester_indices) {
      UInt24 proposer_index = specHelpers
          .get_beacon_proposer_index(state, inclusion_slot.get(index));
      state.withValidatorBalance(proposer_index, balance ->
          balance
              .plus(base_reward.apply(index))
              .dividedBy(spec.getIncluderRewardQuotient()));
    }

    /*
     Crosslinks

     For every i in range(state.slot - 2 * EPOCH_LENGTH, state.slot - EPOCH_LENGTH),
        let shard_committee_at_slot, start_shard = get_shard_committees_at_slot(i).
     For every j in range(len(shard_committee_at_slot)),
        let shard_committee = shard_committee_at_slot[j],
        shard = (start_shard + j) % SHARD_COUNT, and compute:
        // FIXME shard is not used; weird start_shard type

          If index in attesting_validators(shard_committee),
              state.validator_balances[index] += base_reward(state, index) *
                  total_attesting_balance(shard_committee) // total_balance(shard_committee)).
          If index not in attesting_validators(shard_committee),
              state.validator_balances[index] -= base_reward(state, index).
     */
    for (UInt64 i = state.getSlot().minus(spec.getEpochLength().times(2));
        i.compareTo(state.getSlot().minus(spec.getEpochLength())) < 0;
        i = i.increment()) {
      List<Pair<UInt24[], UInt64>> shard_committees_at_slot = specHelpers
          .get_shard_committees_at_slot(state, i);
      for (int j = 0; j < shard_committees_at_slot.size(); j++) {
        UInt24[] shard_committee = shard_committees_at_slot.get(j).getValue0();
        List<UInt24> shard_committee_list = Arrays.asList(shard_committee);
        Set<UInt24> attesting_validator_set = attesting_validators.apply(shard_committee_list);
        for (UInt24 index : shard_committee_list) {
          if (attesting_validator_set.contains(index)) {
            state.withValidatorBalance(index, vb -> vb.plus(base_reward.apply(index)));
          } else {
            state.withValidatorBalance(index, vb -> vb.minus(base_reward.apply(index)));
          }
        }
      }
    }

    /*
    Ejections

    Run process_ejections(state).

    def process_ejections(state: BeaconState) -> None:
      """
      Iterate through the validator registry
      and eject active validators with balance below ``EJECTION_BALANCE``.
      """
      for index in get_active_validator_indices(state.validator_registry, state.slot):
          if state.validator_balances[index] < EJECTION_BALANCE:
              exit_validator(state, index)
     */

    for (UInt24 index :specHelpers.get_active_validator_indices(
            state.getValidatorRegistry(), state.getSlot())) {
      if (state.getValidatorBalances().get(index.getValue())
          .compareTo(spec.getEjectionBalance().toGWei()) < 0) {
        specHelpers.exit_validator(state, index);
      }
    }

    /*
          Validator registry
    */

    //    If the following are satisfied:
    //        state.finalized_slot > state.validator_registry_update_slot and
    //        state.latest_crosslinks[shard].slot > state.validator_registry_update_slot
    //            for every shard number shard in
    //            [(state.current_epoch_start_shard + i) % SHARD_COUNT
    //                for i in range(get_current_epoch_committee_count_per_slot(state) * EPOCH_LENGTH)]
    //     (that is, for every shard in the current committees)
    boolean updateRegistry = state.getFinalizedSlot()
        .compareTo(state.getValidatorRegistryLatestChangeSlot()) > 0;

    UInt64 range = spec.getEpochLength()
        .times(specHelpers.get_current_epoch_committee_count_per_slot(state));
    for (UInt64 i = UInt64.ZERO; i.compareTo(range) < 0; i = i.increment()) {
      UInt64 shard = state.getCurrentEpochStartShard().plus(i).modulo(spec.getShardCount());
      if (state.getLatestCrosslinks().get(shard.getIntValue()).getSlot()
            .compareTo(state.getValidatorRegistryLatestChangeSlot()) <= 0) {
        updateRegistry = false;
        break;
      }
    }

    if (updateRegistry) {
      //    update the validator registry and associated fields by running
      specHelpers.update_validator_registry(state);
      //      Set state.previous_epoch_calculation_slot = state.current_epoch_calculation_slot
      state.withPreviousEpochCalculationSlot(state.getCurrentEpochCalculationSlot());
      //      Set state.previous_epoch_start_shard = state.current_epoch_start_shard
      state.withPreviousEpochStartShard(state.getCurrentEpochStartShard());
      //      Set state.previous_epoch_randao_mix = state.current_epoch_randao_mix
      state.withPreviousEpochRandaoMix(state.getCurrentEpochRandaoMix());
      //      Set state.current_epoch_calculation_slot = state.slot
      state.withCurrentEpochCalculationSlot(state.getSlot());
      //      Set state.current_epoch_start_shard = (state.current_epoch_start_shard +
      //          get_current_epoch_committee_count_per_slot(state) * EPOCH_LENGTH) % SHARD_COUNT
      state.withCurrentEpochStartShard(state.getCurrentEpochStartShard()
          .plus(spec.getEpochLength()
              .times(specHelpers.get_current_epoch_committee_count_per_slot(state)))
          .modulo(spec.getShardCount()));
      //      Set state.current_epoch_randao_mix =
      //        get_randao_mix(state, state.current_epoch_calculation_slot - SEED_LOOKAHEAD)
      state.withCurrentEpochRandaoMix(specHelpers.get_randao_mix(state,
          state.getCurrentEpochCalculationSlot().minus(spec.getSeedLookahead())));
    } else {
      //    If a validator registry update does not happen do the following:

      //    Set state.previous_epoch_calculation_slot = state.current_epoch_calculation_slot
      state.withPreviousEpochCalculationSlot(state.getCurrentEpochCalculationSlot());
      //    Set state.previous_epoch_start_shard = state.current_epoch_start_shard
      state.withPreviousEpochStartShard(state.getCurrentEpochStartShard());
      //    Let epochs_since_last_registry_change =
      //      (state.slot - state.validator_registry_update_slot) // EPOCH_LENGTH.
      UInt64 epochs_since_last_registry_change = state.getSlot()
          .minus(state.getValidatorRegistryLatestChangeSlot())
          .dividedBy(spec.getEpochLength());

      //    If epochs_since_last_registry_change is an exact power of 2,
      if (Long.bitCount(epochs_since_last_registry_change.getValue()) == 1) {
        //      set state.current_epoch_calculation_slot = state.slot and
        state.withCurrentEpochCalculationSlot(state.getSlot());
        //      state.current_epoch_randao_mix = state.latest_randao_mixes
        //        [(state.current_epoch_calculation_slot - SEED_LOOKAHEAD)
        //          % LATEST_RANDAO_MIXES_LENGTH].
        UInt64 idx = state.getCurrentEpochCalculationSlot()
            .minus(spec.getSeedLookahead())
            .modulo(spec.getLatestRandaoMixesLength());
        state.withCurrentEpochRandaoMix(state.getLatestRandaoMixes().get(idx.getIntValue()));
        //    Note that state.current_epoch_start_shard is left unchanged.
      }
    }

    // Regardless of whether or not a validator set change happens, run the following:
    specHelpers.process_penalties_and_exits(state);

    /*
     Final updates
    */

    // Let e = state.slot // EPOCH_LENGTH.
    UInt64 e = state.getSlot().dividedBy(spec.getEpochLength());
    // Set state.latest_penalized_balances[(e+1) % LATEST_PENALIZED_EXIT_LENGTH] =
    //    state.latest_penalized_balances[e % LATEST_PENALIZED_EXIT_LENGTH]
    state.withLatestPenalizedExitBalance(
        e.increment().modulo(spec.getLatestPenalizedExitLength()).getIntValue(),
        ignore -> state.getLatestPenalizedExitBalances()
                .get(e.modulo(spec.getLatestPenalizedExitLength()).getIntValue()));
    // Remove any attestation in state.latest_attestations such that
    //    attestation.data.slot < state.slot - EPOCH_LENGTH.
    List<PendingAttestationRecord> newLatestAttestations = new ArrayList<>();
    for (PendingAttestationRecord attestation : state.getLatestAttestations()) {
      if (attestation.getData().getSlot()
          .compareTo(state.getSlot().minus(spec.getEpochLength())) >= 0) {
        newLatestAttestations.add(attestation);
      }
    }
    state.withLatestAttestations(newLatestAttestations);

    return new BeaconStateEx(state.validate(), stateEx.getLatestChainBlockHash());
  }
}
