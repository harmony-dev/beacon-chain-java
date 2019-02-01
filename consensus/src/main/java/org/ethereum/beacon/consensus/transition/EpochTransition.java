package org.ethereum.beacon.consensus.transition;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.CrosslinkRecord;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
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

    // The steps below happen when (state.slot + 1) % EPOCH_LENGTH == 0.

    /*
      Let current_epoch = get_current_epoch(state).
      Let previous_epoch = current_epoch - 1 if current_epoch > GENESIS_EPOCH else current_epoch.
      Let next_epoch = current_epoch + 1.
     */
    EpochNumber current_epoch = specHelpers.get_current_epoch(state);
    EpochNumber previous_epoch = current_epoch.greater(specHelpers.get_genesis_epoch()) ?
        current_epoch.decrement() : current_epoch;
    EpochNumber next_epoch = current_epoch.increment();

    /*
     Helpers: All validators:
    */

    // Let active_validator_indices =
    //      get_active_validator_indices(state.validator_registry, state.slot).
    List<ValidatorIndex> active_validator_indices = specHelpers.get_active_validator_indices(
        state.getValidatorRegistry(), state.getSlot());

    // Let total_balance = sum([get_effective_balance(state, i) for i in active_validator_indices])
    Gwei total_balance = active_validator_indices.stream()
        .map(i -> specHelpers.get_effective_balance(state, i))
        .reduce(Gwei::plus)
        .orElse(Gwei.ZERO);

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
                    .lessEqual(a.getData().getSlot())
                    && a.getData().getSlot().less(state.getSlot()))
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
    Set<ValidatorIndex> current_epoch_boundary_attester_indices = current_epoch_boundary_attestations
        .stream()
        .flatMap(a ->
            specHelpers
                .get_attestation_participants(state, a.getData(), a.getParticipationBitfield())
                .stream())
        .collect(Collectors.toSet());

    // Let current_epoch_boundary_attesting_balance =
    //    sum([get_effective_balance(state, i) for i in current_epoch_boundary_attester_indices]).
    Gwei current_epoch_boundary_attesting_balance = current_epoch_boundary_attester_indices
        .stream()
        .map(i -> specHelpers.get_effective_balance(state, i))
        .reduce(Gwei::plus)
        .orElse(Gwei.ZERO);

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
                .lessEqual(a.getData().getSlot())
                && a.getData().getSlot()
                .less(state.getSlot().minus(spec.getEpochLength())))
        .collect(Collectors.toList());

    // Let previous_epoch_attester_indices be the union of the validator index sets given by
    //    [get_attestation_participants(state, a.data, a.participation_bitfield)
    //        for a in previous_epoch_attestations]
    Set<ValidatorIndex> previous_epoch_attester_indices = previous_epoch_attestations
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
    Set<ValidatorIndex> previous_epoch_justified_attester_indices = previous_epoch_justified_attestations
        .stream()
        .flatMap(a -> specHelpers.get_attestation_participants(
            state, a.getData(), a.getParticipationBitfield()).stream())
        .collect(Collectors.toSet());

    // Let previous_epoch_justified_attesting_balance = sum([get_effective_balance(state, i)
    //    for i in previous_epoch_justified_attester_indices]).
    Gwei previous_epoch_justified_attesting_balance = previous_epoch_justified_attester_indices
        .stream()
        .map(i -> specHelpers.get_effective_balance(state, i))
        .reduce(Gwei::plus)
        .orElse(Gwei.ZERO);

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
    Set<ValidatorIndex> previous_epoch_boundary_attester_indices = previous_epoch_boundary_attestations
        .stream()
        .flatMap(a -> specHelpers.get_attestation_participants(
            state, a.getData(), a.getParticipationBitfield()).stream())
        .collect(Collectors.toSet());

    // Let previous_epoch_boundary_attesting_balance = sum([get_effective_balance(state, i)
    //    for i in previous_epoch_boundary_attester_indices]).
    Gwei previous_epoch_boundary_attesting_balance = previous_epoch_boundary_attester_indices
        .stream()
        .map(i -> specHelpers.get_effective_balance(state, i))
        .reduce(Gwei::plus)
        .orElse(Gwei.ZERO);

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
    Set<ValidatorIndex> previous_epoch_head_attester_indices = previous_epoch_head_attestations.stream()
        .flatMap(a -> specHelpers.get_attestation_participants(
            state, a.getData(), a.getParticipationBitfield()).stream())
        .collect(Collectors.toSet());

    // Let previous_epoch_head_attesting_balance = sum([get_effective_balance(state, i)
    //    for i in previous_epoch_head_attester_indices]).
    Gwei previous_epoch_head_attesting_balance = previous_epoch_head_attester_indices.stream()
        .map(i -> specHelpers.get_effective_balance(state, i))
        .reduce(Gwei::plus)
        .orElse(Gwei.ZERO);

    Map<Pair<List<ValidatorIndex>, Hash32>, Set<ValidatorIndex>>
        attesting_validator_indices = new HashMap<>();
    Map<List<ValidatorIndex>, Pair<Gwei, Hash32>> winning_root_tmp = new HashMap<>();

    // For every slot in range(state.slot - 2 * EPOCH_LENGTH, state.slot),
    // let shard_committee_at_slot = get_shard_committees_at_slot(slot).
    // For every (shard_committee, shard) in shard_committee_at_slot, compute:
    for (SlotNumber slot : state.getSlot().minus(spec.getEpochLength().times(2))
        .iterateTo(state.getSlot())) {
      List<ShardCommittee> shard_committees_at_slot = specHelpers
          .get_shard_committees_at_slot(state, slot);
      for (ShardCommittee s : shard_committees_at_slot) {
        List<ValidatorIndex> shard_committee = s.getCommittee();
        ShardNumber shard = s.getShard();

        // Let shard_block_root be state.latest_crosslinks[shard].shard_block_root
        Hash32 shard_block_root = state.getLatestCrosslinks().get(shard).getShardBlockRoot();

        // Let attesting_validator_indices(shard_committee, shard_block_root)
        // be the union of the validator index sets given by
        // [get_attestation_participants(state, a.data, a.participation_bitfield)
        //    for a in current_epoch_attestations + previous_epoch_attestations
        //    if a.shard == shard and a.shard_block_root == shard_block_root]
        // FIXME a.shard => a.data.shard
        Set<ValidatorIndex> attesting_validator_indices_tmp = Stream
            .concat(current_epoch_attestations.stream(), previous_epoch_attestations.stream())
            .filter(a -> a.getData().getShard().equals(shard)
                && a.getData().getShardBlockRoot().equals(shard_block_root))
            .flatMap(a -> specHelpers.get_attestation_participants(
                state, a.getData(), a.getParticipationBitfield()).stream())
            .collect(Collectors.toSet());

        attesting_validator_indices.put(
            Pair.with(shard_committee, shard_block_root),
            attesting_validator_indices_tmp);

        // Let winning_root(shard_committee) be equal to the value of shard_block_root
        // such that sum([get_effective_balance(state, i)
        // for i in attesting_validator_indices(shard_committee, shard_block_root)])
        // is maximized (ties broken by favoring lower shard_block_root values).

        // TODO not sure this is correct implementation
        Gwei sum = attesting_validator_indices_tmp.stream()
            .map(i -> specHelpers.get_effective_balance(state, i))
            .reduce(Gwei::plus)
            .orElse(Gwei.ZERO);
        winning_root_tmp.compute(shard_committee, (k, v) ->
            v == null || sum.compareTo(v.getValue0()) > 0 ?
                Pair.with(sum, shard_block_root) : v
        );
      }
    }
    Map<List<ValidatorIndex>, Hash32> winning_root = winning_root_tmp.entrySet().stream()
        .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getValue1()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Let attesting_validators(shard_committee) be equal to
    // attesting_validator_indices(shard_committee, winning_root(shard_committee)) for convenience.
    Function<List<ValidatorIndex>, Set<ValidatorIndex>> attesting_validators = shard_committee ->
        attesting_validator_indices.get(
            Pair.with(shard_committee, winning_root.get(shard_committee)));

    // Let total_attesting_balance(shard_committee) =
    // sum([get_effective_balance(state, i) for i in attesting_validators(shard_committee)])
    Function<List<ValidatorIndex>, Gwei> total_attesting_balance = shard_committee ->
        attesting_validators.apply(shard_committee).stream()
            .map(i -> specHelpers.get_effective_balance(state, i))
            .reduce(Gwei::plus)
            .orElse(Gwei.ZERO);

    // Let total_balance(shard_committee) = sum([get_effective_balance(state, i)
    //    for i in shard_committee]).
    // FIXME total_balance name conflict
    Function<List<ValidatorIndex>, Gwei> total_balance_func = shard_committee ->
        shard_committee.stream()
            .map(i -> specHelpers.get_effective_balance(state, i))
            .reduce(Gwei::plus)
            .orElse(Gwei.ZERO);

    // Define the following helpers to process attestation inclusion rewards
    //  and inclusion distance reward/penalty

    // For every attestation a in previous_epoch_attestations:
    //    Let inclusion_slot(state, index) = a.slot_included for the attestation a
    //      where index is in get_attestation_participants(state, a.data, a.participation_bitfield).
    //    Let inclusion_distance(state, index) = a.slot_included - a.data.slot
    //      where a is the above attestation. FIXME fuzzy spec
    Map<ValidatorIndex, SlotNumber> inclusion_slot = new HashMap<>();
    Map<ValidatorIndex, SlotNumber> inclusion_distance = new HashMap<>();
    for (PendingAttestationRecord a : previous_epoch_attestations) {
      List<ValidatorIndex> attestation_participants = specHelpers
          .get_attestation_participants(state, a.getData(), a.getParticipationBitfield());
      for (ValidatorIndex participant : attestation_participants) {
        inclusion_slot.put(participant, a.getInclusionSlot());
        inclusion_distance.put(participant, a.getInclusionSlot().minus(a.getData().getSlot()));
      }
    }

    /*
     Eth1 data

     Set state.latest_eth1_data = eth1_data_vote.data if eth1_data_vote.vote_count * 2 >
     ETH1_DATA_VOTING_PERIOD for some eth1_data_vote in state.eth1_data_votes.
     Set state.eth1_data_votes = [].
    */
    for (Eth1DataVote eth1_data_vote : state.getEth1DataVotes()) {
      if (eth1_data_vote.getVoteCount().times(2)
          .compareTo(spec.getEth1DataVotingPeriod()) > 0) {
        state.setLatestEth1Data(eth1_data_vote.getEth1Data());
        break;
      }
    }
    state.getEth1DataVotes().clear();

    /*
     Justification
    */

    // Set state.previous_justified_slot = state.justified_slot.
    state.setPreviousJustifiedSlot(state.getJustifiedSlot());

    // Set state.justification_bitfield = (state.justification_bitfield * 2) % 2**64.
    state.setJustificationBitfield(state.getJustificationBitfield().shl(1));

    // Set state.justification_bitfield |= 2 and
    //    state.justified_slot = state.slot - 2 * EPOCH_LENGTH
    // if 3 * previous_epoch_boundary_attesting_balance >= 2 * total_balance.
    if (previous_epoch_boundary_attesting_balance.times(3).greaterEqual(
            total_balance.times(2))) {
      state.setJustificationBitfield(state.getJustificationBitfield().or(2));
      state.setJustifiedSlot(state.getSlot().minus(spec.getEpochLength().times(2)));
    }

    // Set state.justification_bitfield |= 1 and
    //    state.justified_slot = state.slot - 1 * EPOCH_LENGTH
    // if 3 * current_epoch_boundary_attesting_balance >= 2 * total_balance.
    if (current_epoch_boundary_attesting_balance.times(3).greaterEqual(
            total_balance.times(2))) {
      state.setJustificationBitfield(state.getJustificationBitfield().or(1));
      state.setJustifiedSlot(state.getSlot().minus(spec.getEpochLength().times(1)));
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
      state.setFinalizedSlot(state.getPreviousJustifiedSlot());
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

    /*
     For every slot in range(get_epoch_start_slot(previous_epoch), get_epoch_start_slot(next_epoch)),
     let crosslink_committees_at_slot = get_crosslink_committees_at_slot(state, slot).
     For every (crosslink_committee, shard) in crosslink_committees_at_slot, compute:

         Set state.latest_crosslinks[shard] = Crosslink(
           epoch=current_epoch,
           shard_block_root=winning_root(crosslink_committee))
         if 3 * total_attesting_balance(crosslink_committee) >=
               2 * total_balance(crosslink_committee).

    */

    spec.getEpochLength().times(2).streamFromZero()
        .map(i -> state.getSlot().minus(spec.getEpochLength().times(2)).plus(i))
        .flatMap(slot -> specHelpers.get_shard_committees_at_slot(state, slot).stream())
        .filter(shardCom ->
          total_attesting_balance.apply(shardCom.getCommittee()).times(3)
              .greaterEqual(total_balance_func.apply(shardCom.getCommittee()).times(3)))
        .forEachOrdered(shardCommittee -> {
          state.getLatestCrosslinks().set(shardCommittee.getShard(),
              new CrosslinkRecord(
                  state.getSlot(),
                  winning_root.get(shardCommittee.getCommittee())));
        });

    /*
     Rewards and penalties
     First, we define some additional helpers:
     */

    //     Let base_reward_quotient = BASE_REWARD_QUOTIENT *
    //        integer_squareroot(total_balance // GWEI_PER_ETH)
    Gwei base_reward_quotient = Gwei.castFrom(spec.getBaseRewardQuotient()
        .times(specHelpers.integer_squareroot(total_balance)));

    // Let base_reward(state, index) = get_effective_balance(state, index) //
    //    base_reward_quotient // 5 for any validator with the given index.
    Function<ValidatorIndex, Gwei> base_reward = index ->
        specHelpers.get_effective_balance(state, index)
            .dividedBy(base_reward_quotient)
            .dividedBy(5);

    // Let inactivity_penalty(state, index, epochs_since_finality) = base_reward(state, index) +
    // get_effective_balance(state, index) * epochs_since_finality //
    // INACTIVITY_PENALTY_QUOTIENT // 2 for any validator with the given index.

    BiFunction<ValidatorIndex, EpochNumber, Gwei> inactivity_penalty =
        (index, epochs_since_finality) ->
        base_reward.apply(index).plus(
            specHelpers.get_effective_balance(state, index)
                .times(epochs_since_finality)
                .dividedBy(spec.getInactivityPenaltyQuotient())
                .dividedBy(2));

    /*
     Justification and finalization
    */

    //    Let epochs_since_finality = (state.slot - state.finalized_slot) // EPOCH_LENGTH.
    EpochNumber epochs_since_finality = state.getSlot()
        .minus(state.getFinalizedSlot())
        .dividedBy(spec.getEpochLength());

    if (epochs_since_finality.lessEqual(EpochNumber.of(4))) {
      // Case 1: epochs_since_finality <= 4:

      //  Expected FFG source:

      //  Any validator index in previous_epoch_justified_attester_indices gains
      //    base_reward(state, index) * previous_epoch_justified_attesting_balance // total_balance.
      for (ValidatorIndex index : previous_epoch_justified_attester_indices) {
        state.getValidatorBalances().update(index, balance ->
            balance.plus(base_reward.apply(index)
                    .times(previous_epoch_justified_attesting_balance)
                    .dividedBy(total_balance)));
      }
      //  Any active validator v not in previous_epoch_justified_attester_indices loses
      //    base_reward(state, index).
      //  FIXME 'active validator' - not exact meaning
      for (ValidatorIndex index : active_validator_indices) {
        if (!previous_epoch_justified_attester_indices.contains(index)) {
          state.getValidatorBalances().update(index, balance ->
              balance.minus(base_reward.apply(index)));
        }
      }

      //  Expected FFG target:

      //  Any validator index in previous_epoch_boundary_attester_indices gains
      //    base_reward(state, index) * previous_epoch_boundary_attesting_balance // total_balance.
      for (ValidatorIndex index : previous_epoch_boundary_attester_indices) {
        state.getValidatorBalances().update(index, balance ->
            balance
                .plus(base_reward.apply(index)
                    .times(previous_epoch_boundary_attesting_balance)
                    .dividedBy(total_balance)));
      }
      //  Any active validator index not in previous_epoch_boundary_attester_indices loses
      //    base_reward(state, index).
      for (ValidatorIndex index : active_validator_indices) {
        if (!previous_epoch_boundary_attester_indices.contains(index)) {
          state.getValidatorBalances().update(index, balance ->
              balance.minus(base_reward.apply(index)));
        }
      }

      //  Expected beacon chain head:

      //  Any validator index in previous_epoch_head_attester_indices gains
      //    base_reward(state, index) * previous_epoch_head_attesting_balance // total_balance).
      for (ValidatorIndex index : previous_epoch_head_attester_indices) {
        state.getValidatorBalances().update(index, balance ->
            balance.plus(base_reward.apply(index)
                    .times(previous_epoch_head_attesting_balance)
                    .dividedBy(total_balance)));
      }
      //  Any active validator index not in previous_epoch_head_attester_indices loses
      //    base_reward(state, index).
      for (ValidatorIndex index : active_validator_indices) {
        if (!previous_epoch_head_attester_indices.contains(index)) {
          state.getValidatorBalances().update(index, balance ->
              balance.minus(base_reward.apply(index)));
        }
      }

      //  Inclusion distance:

      // Any validator index in previous_epoch_attester_indices gains
      //    base_reward(state, index) * MIN_ATTESTATION_INCLUSION_DELAY //
      //        inclusion_distance(state, index)
      for (ValidatorIndex index : previous_epoch_attester_indices) {
        state.getValidatorBalances().update(index, balance ->
            balance.plus(base_reward.apply(index)
                    .times(spec.getMinAttestationInclusionDelay())
                    .dividedBy(inclusion_distance.get(index))));
      }
    } else {
      // Case 2: epochs_since_finality > 4:

      //  Any active validator index not in previous_epoch_justified_attester_indices, loses
      //      inactivity_penalty(state, index, epochs_since_finality).
      for (ValidatorIndex index : active_validator_indices) {
        if (!previous_epoch_justified_attester_indices.contains(index)) {
          state.getValidatorBalances().update(index, balance ->
              balance.minus(inactivity_penalty.apply(index, epochs_since_finality)));
        }
      }
      //  Any active validator index not in previous_epoch_boundary_attester_indices, loses
      //      inactivity_penalty(state, index, epochs_since_finality).
      for (ValidatorIndex index : active_validator_indices) {
        if (!previous_epoch_boundary_attester_indices.contains(index)) {
          state.getValidatorBalances().update(index, balance ->
              balance.minus(inactivity_penalty.apply(index, epochs_since_finality)));
        }
      }
      //  Any active validator index not in previous_epoch_head_attester_indices, loses
      //      base_reward(state, index).
      for (ValidatorIndex index : active_validator_indices) {
        if (!previous_epoch_head_attester_indices.contains(index)) {
          state.getValidatorBalances().update(index, balance ->
              balance.minus(base_reward.apply(index)));
        }
      }
      //  Any active_validator index with validator.penalized_slot <= state.slot, loses
      //      2 * inactivity_penalty(state, index, epochs_since_finality) + base_reward(state, index).
      for (ValidatorIndex index : active_validator_indices) {
        ValidatorRecord validator = state.getValidatorRegistry().get(index);
        if (validator.getPenalizedSlot().compareTo(state.getSlot()) <= 0) {
          state.getValidatorBalances().update(index, balance ->
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
      for (ValidatorIndex index : previous_epoch_attester_indices) {
        state.getValidatorBalances().update(index, balance -> balance.minus(
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
    for (ValidatorIndex index : previous_epoch_attester_indices) {
      ValidatorIndex proposer_index = specHelpers
          .get_beacon_proposer_index(state, inclusion_slot.get(index));
      state.getValidatorBalances().update(proposer_index, balance ->
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
    for (SlotNumber i : state.getSlot().minus(spec.getEpochLength().times(2)).
            iterateTo(state.getSlot().minus(spec.getEpochLength()))) {
      List<ShardCommittee> shard_committees_at_slot = specHelpers
          .get_shard_committees_at_slot(state, i);
      for (int j = 0; j < shard_committees_at_slot.size(); j++) {
        List<ValidatorIndex> shard_committee = shard_committees_at_slot.get(j).getCommittee();
        Set<ValidatorIndex> attesting_validator_set = attesting_validators.apply(shard_committee);
        for (ValidatorIndex index : shard_committee) {
          if (attesting_validator_set.contains(index)) {
            state.getValidatorBalances().update(index, vb -> vb.plus(base_reward.apply(index)));
          } else {
            state.getValidatorBalances().update(index, vb -> vb.minus(base_reward.apply(index)));
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

    for (ValidatorIndex index :specHelpers.get_active_validator_indices(
            state.getValidatorRegistry(), state.getSlot())) {
      if (state.getValidatorBalances().get(index).less(spec.getEjectionBalance())) {
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

    SlotNumber range = spec.getEpochLength()
        .times(specHelpers.get_current_epoch_committee_count_per_slot(state));
    for (SlotNumber i : SlotNumber.of(0).iterateTo(range)) {
      ShardNumber shard = state.getCurrentEpochStartShard().plusModulo(i, spec.getShardCount());
      if (state.getLatestCrosslinks().get(shard).getSlot().lessEqual(
          state.getValidatorRegistryLatestChangeSlot())) {
        updateRegistry = false;
        break;
      }
    }

    if (updateRegistry) {
      //    update the validator registry and associated fields by running
      specHelpers.update_validator_registry(state);
      //      Set state.previous_epoch_calculation_slot = state.current_epoch_calculation_slot
      state.setPreviousEpochCalculationSlot(state.getCurrentEpochCalculationSlot());
      //      Set state.previous_epoch_start_shard = state.current_epoch_start_shard
      state.setPreviousEpochStartShard(state.getCurrentEpochStartShard());
      //      Set state.previous_epoch_randao_mix = state.current_epoch_randao_mix
      state.setPreviousEpochRandaoMix(state.getCurrentEpochRandaoMix());
      //      Set state.current_epoch_calculation_slot = state.slot
      state.setCurrentEpochCalculationSlot(state.getSlot());
      //      Set state.current_epoch_start_shard = (state.current_epoch_start_shard +
      //          get_current_epoch_committee_count_per_slot(state) * EPOCH_LENGTH) % SHARD_COUNT
      state.setCurrentEpochStartShard(state.getCurrentEpochStartShard()
          .plusModulo(
              spec.getEpochLength().times(specHelpers.get_current_epoch_committee_count_per_slot(state)),
              spec.getShardCount()));
      //      Set state.current_epoch_randao_mix =
      //        get_randao_mix(state, state.current_epoch_calculation_slot - SEED_LOOKAHEAD)
      state.setCurrentEpochRandaoMix(specHelpers.get_randao_mix(state,
          state.getCurrentEpochCalculationSlot().minus(spec.getSeedLookahead())));
    } else {
      //    If a validator registry update does not happen do the following:

      //    Set state.previous_epoch_calculation_slot = state.current_epoch_calculation_slot
      state.setPreviousEpochCalculationSlot(state.getCurrentEpochCalculationSlot());
      //    Set state.previous_epoch_start_shard = state.current_epoch_start_shard
      state.setPreviousEpochStartShard(state.getCurrentEpochStartShard());
      //    Let epochs_since_last_registry_change =
      //      (state.slot - state.validator_registry_update_slot) // EPOCH_LENGTH.
      EpochNumber epochs_since_last_registry_change = state.getSlot()
          .minus(state.getValidatorRegistryLatestChangeSlot())
          .dividedBy(spec.getEpochLength());

      //    If epochs_since_last_registry_change is an exact power of 2,
      if (Long.bitCount(epochs_since_last_registry_change.getValue()) == 1) {
        //      set state.current_epoch_calculation_slot = state.slot and
        state.setCurrentEpochCalculationSlot(state.getSlot());
        //      state.current_epoch_randao_mix = state.latest_randao_mixes
        //        [(state.current_epoch_calculation_slot - SEED_LOOKAHEAD)
        //          % LATEST_RANDAO_MIXES_LENGTH].
        UInt64 idx = state.getCurrentEpochCalculationSlot()
            .minus(spec.getSeedLookahead())
            .modulo(spec.getLatestRandaoMixesLength());
        state.setCurrentEpochRandaoMix(state.getLatestRandaoMixes().get(idx));
        //    Note that state.current_epoch_start_shard is left unchanged.
      }
    }

    // Regardless of whether or not a validator set change happens, run the following:
    specHelpers.process_penalties_and_exits(state);

    /*
     Final updates
    */

    // Let e = state.slot // EPOCH_LENGTH.
    EpochNumber e = state.getSlot().dividedBy(spec.getEpochLength());
    // Set state.latest_penalized_balances[(e+1) % LATEST_PENALIZED_EXIT_LENGTH] =
    //    state.latest_penalized_balances[e % LATEST_PENALIZED_EXIT_LENGTH]
    state.getLatestPenalizedExitBalances().update(
        e.increment().modulo(spec.getLatestPenalizedExitLength()),
        ignore -> state.getLatestPenalizedExitBalances()
                .get(e.modulo(spec.getLatestPenalizedExitLength())));
    // Remove any attestation in state.latest_attestations such that
    //    attestation.data.slot < state.slot - EPOCH_LENGTH.
    state.getLatestAttestations().clear();
    for (PendingAttestationRecord attestation : state.getLatestAttestations()) {
      if (attestation.getData().getSlot()
          .compareTo(state.getSlot().minus(spec.getEpochLength())) >= 0) {
        state.getLatestAttestations().add(attestation);
      }
    }

    return new BeaconStateEx(state.createImmutable(), stateEx.getLatestChainBlockHash());
  }
}
