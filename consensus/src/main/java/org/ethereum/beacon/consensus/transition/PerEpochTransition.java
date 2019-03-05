package org.ethereum.beacon.consensus.transition;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
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

/**
 * Per-epoch transition function.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/core/0_beacon-chain.md#per-epoch-processing">Per-epoch
 *     processing</a> in the spec.
 */
public class PerEpochTransition implements StateTransition<BeaconStateEx> {
  private static final Logger logger = LogManager.getLogger(PerEpochTransition.class);

  private final SpecHelpers spec;

  public PerEpochTransition(SpecHelpers spec) {
    this.spec = spec;
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx stateEx) {
    return apply(stateEx, null);
  }

  public EpochTransitionSummary applyWithSummary(BeaconStateEx stateEx) {
    EpochTransitionSummary summary = new EpochTransitionSummary();
    apply(stateEx, summary);
    return summary;
  }

  private BeaconStateEx apply(BeaconStateEx origState, EpochTransitionSummary summary) {
    logger.debug(() -> "Applying epoch transition to state: (" +
        spec.hash_tree_root(origState).toStringShort() + ") " +
        origState.toString(spec.getConstants()));

    TransitionType.EPOCH.checkCanBeAppliedAfter(origState.getTransition());

    if (summary != null) {
      summary.preState = origState;
    }
    MutableBeaconState state = origState.createMutableCopy();

    // The steps below happen when (state.slot + 1) % SLOTS_PER_EPOCH == 0.

    /*
      Let current_epoch = get_current_epoch(state).
      Let previous_epoch = get_previous_epoch(state).
      Let next_epoch = current_epoch + 1.
     */
    EpochNumber current_epoch = spec.get_current_epoch(state);
    EpochNumber previous_epoch = spec.get_previous_epoch(state);
    EpochNumber next_epoch = current_epoch.increment();

    /*
     Helpers: All validators:
    */

    /*
     Helpers: Validators attesting during the current epoch:
    */

    // Let current_total_balance = get_total_balance(state,
    //      get_active_validator_indices(state.validator_registry, current_epoch)).
    List<ValidatorIndex> current_active_validator_indices = spec.get_active_validator_indices(
        state.getValidatorRegistry(), current_epoch);
    Gwei current_total_balance = spec.get_total_balance(state, current_active_validator_indices);
    logger.trace(() -> current_active_validator_indices.size() + " active validators with total balance " + current_total_balance);

    // Let current_epoch_attestations =
    //   [a for a in state.latest_attestations if current_epoch == slot_to_epoch(a.data.slot)].
    // (Note: this is the set of attestations of slots in the epoch current_epoch,
    //  not attestations that got included in the chain during the epoch current_epoch.)
    List<PendingAttestationRecord> current_epoch_attestations =
        state.getLatestAttestations().stream()
            .filter(a -> current_epoch.equals(spec.slot_to_epoch(a.getData().getSlot())))
            .collect(Collectors.toList());

    // Validators justifying the epoch boundary block at the start of the current epoch:

    // Let current_epoch_boundary_attestations = [a for a in current_epoch_attestations
    //      if a.data.epoch_boundary_root == get_block_root(state, get_epoch_start_slot(current_epoch)).
    List<PendingAttestationRecord> current_epoch_boundary_attestations =
        current_epoch_attestations.stream().filter(a ->
            a.getData().getEpochBoundaryRoot().equals(
                spec.get_block_root(state, spec.get_epoch_start_slot(current_epoch)))
        ).collect(Collectors.toList());

    // Let current_epoch_boundary_attester_indices be the union of the validator index sets
    // given by [get_attestation_participants(state, a.data, a.aggregation_bitfield)
    //    for a in current_epoch_boundary_attestations].
    Set<ValidatorIndex> current_epoch_boundary_attester_indices = current_epoch_boundary_attestations
        .stream()
        .flatMap(a ->
            spec
                .get_attestation_participants(state, a.getData(), a.getAggregationBitfield())
                .stream())
        .collect(Collectors.toSet());


    // Let current_epoch_boundary_attesting_balance =
    //    get_total_balance(state, current_epoch_boundary_attester_indices).
    Gwei current_epoch_boundary_attesting_balance = spec.get_total_balance(state,
        current_epoch_boundary_attester_indices);

    if (summary != null) {
      summary.currentEpochSummary.activeAttesters = current_active_validator_indices;
      summary.currentEpochSummary.validatorBalance = current_total_balance;
      summary.currentEpochSummary.boundaryAttesters.addAll(current_epoch_boundary_attester_indices);
      summary.currentEpochSummary.boundaryAttestingBalance = current_epoch_boundary_attesting_balance;
    }
    /*
     Helpers: Validators attesting during the previous epoch:
    */

    List<ValidatorIndex> previous_active_validator_indices = spec
        .get_active_validator_indices(state.getValidatorRegistry(), previous_epoch);

    // Let previous_total_balance = get_total_balance(state,
    //    get_active_validator_indices(state.validator_registry, previous_epoch)).
    Gwei previous_total_balance = spec.get_total_balance(state, previous_active_validator_indices);

    // Validators that made an attestation during the previous epoch:

    // Let previous_epoch_attestations = [a for a in state.latest_attestations
    //    if previous_epoch == slot_to_epoch(a.data.slot)].
    List<PendingAttestationRecord> previous_epoch_attestations = state.getLatestAttestations()
        .stream()
        .filter(a -> previous_epoch.equals(spec.slot_to_epoch(a.getData().getSlot())))
        .collect(Collectors.toList());

    // Let previous_epoch_attester_indices be the union of the validator index sets given by
    //    [get_attestation_participants(state, a.data, a.aggregation_bitfield)
    //        for a in previous_epoch_attestations]
    Set<ValidatorIndex> previous_epoch_attester_indices = previous_epoch_attestations
        .stream()
        .flatMap(a -> spec
            .get_attestation_participants(state, a.getData(), a.getAggregationBitfield())
            .stream())
        .collect(Collectors.toSet());

    // Validators targeting the previous justified slot:

    // Let previous_epoch_attesting_balance =
    //    get_total_balance(state, previous_epoch_attester_indices).
    Gwei previous_epoch_attesting_balance =
        spec.get_total_balance(state, previous_epoch_attester_indices);

    // Validators justifying the epoch boundary block at the start of the previous epoch:

    // Let previous_epoch_boundary_attestations = [a for a in previous_epoch_attestations
    //    if a.data.epoch_boundary_root == get_block_root(state, get_epoch_start_slot(previous_epoch))].
    List<PendingAttestationRecord> previous_epoch_boundary_attestations =
        previous_epoch_attestations.stream()
            .filter(a -> a.getData().getEpochBoundaryRoot()
                .equals(spec.get_block_root(state, spec.get_epoch_start_slot(previous_epoch))))
            .collect(Collectors.toList());

    // Let previous_epoch_boundary_attester_indices be the union of the validator index sets
    // given by [get_attestation_participants(state, a.data, a.aggregation_bitfield)
    //    for a in previous_epoch_boundary_attestations]
    Set<ValidatorIndex> previous_epoch_boundary_attester_indices = previous_epoch_boundary_attestations
        .stream()
        .flatMap(a -> spec.get_attestation_participants(
            state, a.getData(), a.getAggregationBitfield()).stream())
        .collect(Collectors.toSet());

    // Let previous_epoch_boundary_attesting_balance =
    //    get_total_balance(state, previous_epoch_boundary_attester_indices).
    Gwei previous_epoch_boundary_attesting_balance =
        spec.get_total_balance(state, previous_epoch_boundary_attester_indices);

    // Validators attesting to the expected beacon chain head during the previous epoch:

    // Let previous_epoch_head_attestations = [a for a in previous_epoch_attestations
    //    if a.data.beacon_block_root == get_block_root(state, a.data.slot)].
    List<PendingAttestationRecord> previous_epoch_head_attestations = previous_epoch_attestations
        .stream()
        .filter(a -> a.getData().getBeaconBlockRoot()
            .equals(spec.get_block_root(state, a.getData().getSlot())))
        .collect(Collectors.toList());

    // Let previous_epoch_head_attester_indices be the union of the validator index sets given by
    // [get_attestation_participants(state, a.data, a.aggregation_bitfield)
    //    for a in previous_epoch_head_attestations].
    Set<ValidatorIndex> previous_epoch_head_attester_indices = previous_epoch_head_attestations.stream()
        .flatMap(a -> spec.get_attestation_participants(
            state, a.getData(), a.getAggregationBitfield()).stream())
        .collect(Collectors.toSet());

    // Let previous_epoch_head_attesting_balance =
    //    get_total_balance(state, previous_epoch_head_attester_indices).
    Gwei previous_epoch_head_attesting_balance =
        spec.get_total_balance(state, previous_epoch_head_attester_indices);

    if (summary != null) {
      summary.previousEpochSummary.activeAttesters = current_active_validator_indices;
      summary.previousEpochSummary.validatorBalance = current_total_balance;
      summary.previousEpochSummary.boundaryAttesters.addAll(previous_epoch_boundary_attester_indices);
      summary.previousEpochSummary.boundaryAttestingBalance = previous_epoch_boundary_attesting_balance;
      summary.headAttesters.addAll(previous_epoch_head_attester_indices);
      summary.headAttestingBalance = previous_epoch_head_attesting_balance;
      summary.justifiedAttesters.addAll(previous_epoch_attester_indices);
      summary.justifiedAttestingBalance = previous_epoch_attesting_balance;
    }


    Map<Pair<List<ValidatorIndex>, Hash32>, Set<ValidatorIndex>>
        attesting_validator_indices = new HashMap<>();
    Map<List<ValidatorIndex>, Pair<Gwei, Hash32>> winning_root_tmp = new HashMap<>();

    // For every slot in range(get_epoch_start_slot(previous_epoch), get_epoch_start_slot(next_epoch)),
    // let crosslink_committees_at_slot = get_crosslink_committees_at_slot(state, slot).
    // For every (crosslink_committee, shard) in crosslink_committees_at_slot, compute:
    for (SlotNumber slot : spec.get_epoch_start_slot(previous_epoch)
                .iterateTo(spec.get_epoch_start_slot(next_epoch))) {
      for (ShardCommittee s : spec.get_crosslink_committees_at_slot(state, slot)) {
        List<ValidatorIndex> crosslink_committee = s.getCommittee();
        ShardNumber shard = s.getShard();

        // Let crosslink_data_root be state.latest_crosslinks[shard].crosslink_data_root
        Hash32 crosslink_data_root = state.getLatestCrosslinks().get(shard).getCrosslinkDataRoot();
        // Let attesting_validator_indices(crosslink_committee, crosslink_data_root)
        // be the union of the validator index sets given by
        // [get_attestation_participants(state, a.data, a.aggregation_bitfield)
        //    for a in current_epoch_attestations + previous_epoch_attestations
        //    if a.data.shard == shard and a.data.crosslink_data_root == crosslink_data_root].
        Set<ValidatorIndex> attesting_validator_indices_tmp = Stream
            .concat(current_epoch_attestations.stream(), previous_epoch_attestations.stream())
            .filter(a -> a.getData().getShard().equals(shard)
                && a.getData().getCrosslinkDataRoot().equals(crosslink_data_root))
            .flatMap(a -> spec.get_attestation_participants(
                state, a.getData(), a.getAggregationBitfield()).stream())
            .collect(Collectors.toSet());

        attesting_validator_indices.put(
            Pair.with(crosslink_committee, crosslink_data_root),
            attesting_validator_indices_tmp);

        // Let winning_root(crosslink_committee) be equal to the value of crosslink_data_root
        // such that sum([get_effective_balance(state, i)
        // for i in attesting_validator_indices(crosslink_committee, crosslink_data_root)])
        // is maximized (ties broken by favoring lexicographically smallest crosslink_data_root values).
        // TODO not sure this is correct implementation
        Gwei sum = attesting_validator_indices_tmp.stream()
            .map(i -> spec.get_effective_balance(state, i))
            .reduce(Gwei::plus)
            .orElse(Gwei.ZERO);
        winning_root_tmp.compute(crosslink_committee, (k, v) -> {
            if (v == null) {
              return Pair.with(sum, crosslink_data_root);
            }
            if (sum.greater(v.getValue0())) {
              return Pair.with(sum, crosslink_data_root);
            }
            if (sum.equals(v.getValue0()) && crosslink_data_root.compareTo(v.getValue1()) > 0) {
              return Pair.with(sum, crosslink_data_root);
            }

            return v;
          }
        );
      }
    }
    Map<List<ValidatorIndex>, Hash32> winning_root = winning_root_tmp.entrySet().stream()
        .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getValue1()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Let attesting_validators(crosslink_committee) be equal to
    // attesting_validator_indices(crosslink_committee, winning_root(crosslink_committee)) for convenience.
    Function<List<ValidatorIndex>, Set<ValidatorIndex>> attesting_validators = crosslink_committee ->
        attesting_validator_indices.get(
            Pair.with(crosslink_committee, winning_root.get(crosslink_committee)));

    // Let total_attesting_balance(crosslink_committee) =
    // sum([get_effective_balance(state, i) for i in attesting_validators(crosslink_committee)]).
    Function<List<ValidatorIndex>, Gwei> total_attesting_balance = crosslink_committee ->
        attesting_validators.apply(crosslink_committee).stream()
            .map(i -> spec.get_effective_balance(state, i))
            .reduce(Gwei::plus)
            .orElse(Gwei.ZERO);

    // Let total_balance(crosslink_committee) = sum([get_effective_balance(state, i)
    //    for i in crosslink_committee]).
    Function<List<ValidatorIndex>, Gwei> total_balance = crosslink_committee ->
        crosslink_committee.stream()
            .map(i -> spec.get_effective_balance(state, i))
            .reduce(Gwei::plus)
            .orElse(Gwei.ZERO);

    // Define the following helpers to process attestation inclusion rewards
    //  and inclusion distance reward/penalty

    // For every attestation a in previous_epoch_attestations:
    //    Let inclusion_slot(state, index) = a.slot_included for the attestation a
    //      where index is in get_attestation_participants(state, a.data, a.aggregation_bitfield).
    //    If multiple attestations are applicable,
    //      the attestation with lowest inclusion_slot is considered.
    //    Let inclusion_distance(state, index) = a.slot_included - a.data.slot
    //      where a is the above attestation.
    Map<ValidatorIndex, SlotNumber> inclusion_slot = new HashMap<>();
    Map<ValidatorIndex, SlotNumber> inclusion_distance = new HashMap<>();
    for (PendingAttestationRecord a : previous_epoch_attestations) {
      List<ValidatorIndex> attestation_participants = spec
          .get_attestation_participants(state, a.getData(), a.getAggregationBitfield());
      for (ValidatorIndex participant : attestation_participants) {
        if (inclusion_slot.get(participant) == null ||
            inclusion_slot.get(participant).greater(a.getInclusionSlot())) {
          inclusion_slot.put(participant, a.getInclusionSlot());
          inclusion_distance.put(participant, a.getInclusionSlot().minus(a.getData().getSlot()));
        }
      }
    }

    /*
     Eth1 data

     If next_epoch % EPOCHS_PER_ETH1_VOTING_PERIOD == 0:

      If eth1_data_vote.vote_count * 2 >
      EPOCHS_PER_ETH1_VOTING_PERIOD * SLOTS_PER_EPOCH for some eth1_data_vote in state.eth1_data_votes
      (ie. more than half the votes in this voting period were for that value),
      set state.latest_eth1_data = eth1_data_vote.eth1_data.
      Set state.eth1_data_votes = [].
    */
    if (next_epoch.modulo(spec.getConstants().getEth1DataVotingPeriod()).equals(EpochNumber.ZERO)) {
      for (Eth1DataVote eth1_data_vote : state.getEth1DataVotes()) {
        if (SlotNumber.castFrom(eth1_data_vote.getVoteCount().times(2))
            .greater(spec.getConstants().getEth1DataVotingPeriod().mul(spec.getConstants().getSlotsPerEpoch()))) {
          state.setLatestEth1Data(eth1_data_vote.getEth1Data());
          logger.debug(() -> "Latest Eth1Data changed to " + state.getLatestEth1Data());
          break;
        }
      }
      state.getEth1DataVotes().clear();
    }

    /*
     Justification
    */

    // First, update the justification bitfield:

    // Let new_justified_epoch = state.justified_epoch.
    EpochNumber new_justified_epoch = state.getJustifiedEpoch();

    // Set state.justification_bitfield = state.justification_bitfield << 1.
    state.setJustificationBitfield(state.getJustificationBitfield().shl(1));

    // Set state.justification_bitfield |= 2 and new_justified_epoch = previous_epoch
    //    if 3 * previous_epoch_boundary_attesting_balance >= 2 * previous_total_balance.
    if (previous_epoch_boundary_attesting_balance.times(3).greaterEqual(
            previous_total_balance.times(2))) {
      state.setJustificationBitfield(state.getJustificationBitfield().or(2));
      new_justified_epoch = previous_epoch;
    }

    // Set state.justification_bitfield |= 1 and new_justified_epoch = current_epoch
    //    if 3 * current_epoch_boundary_attesting_balance >= 2 * current_total_balance.

    // Set state.justification_bitfield |= 1 and
    //    state.justified_slot = state.slot - 1 * SLOTS_PER_EPOCH
    // if 3 * current_epoch_boundary_attesting_balance >= 2 * total_balance.
    if (current_epoch_boundary_attesting_balance.times(3).greaterEqual(
            current_total_balance.times(2))) {
      state.setJustificationBitfield(state.getJustificationBitfield().or(1));
      new_justified_epoch = current_epoch;
    }

    // Next, update last finalized epoch if possible:

    // Set state.finalized_epoch = state.previous_justified_epoch
    //    if (state.justification_bitfield >> 1) % 8 == 0b111 and
    //        state.previous_justified_epoch == previous_epoch - 2.
    if (state.getJustificationBitfield().shr(1).modulo(8).equals(UInt64.valueOf(0b111))
        && state.getPreviousJustifiedEpoch().equals(previous_epoch.minus(2))) {
      state.setFinalizedEpoch(state.getPreviousJustifiedEpoch());
    }
    // Set state.finalized_epoch = state.previous_justified_epoch
    //    if (state.justification_bitfield >> 1) % 4 == 0b11 and
    //        state.previous_justified_epoch == previous_epoch - 1.
    if (state.getJustificationBitfield().shr(1).modulo(4).equals(UInt64.valueOf(0b11))
        && state.getPreviousJustifiedEpoch().equals(previous_epoch.minus(1))) {
      state.setFinalizedEpoch(state.getPreviousJustifiedEpoch());
    }
    // Set state.finalized_epoch = state.justified_epoch
    //    if (state.justification_bitfield >> 0) % 8 == 0b111 and
    //        state.justified_epoch == previous_epoch - 1.
    if (state.getJustificationBitfield().shr(0).modulo(8).equals(UInt64.valueOf(0b111))
        && state.getJustifiedEpoch().equals(previous_epoch.minus(1))) {
      state.setFinalizedEpoch(state.getJustifiedEpoch());
    }
    // Set state.finalized_epoch = state.justified_epoch
    //    if (state.justification_bitfield >> 0) % 4 == 0b11 and
    //        state.justified_epoch == previous_epoch.
    if (state.getJustificationBitfield().shr(0).modulo(4).equals(UInt64.valueOf(0b11))
        && state.getJustifiedEpoch().equals(previous_epoch)) {
      state.setFinalizedEpoch(state.getJustifiedEpoch());
    }

    // Finally, update the following:
    // Set state.previous_justified_epoch = state.justified_epoch.
    state.setPreviousJustifiedEpoch(state.getJustifiedEpoch());
    // Set state.justified_epoch = new_justified_epoch.
    state.setJustifiedEpoch(new_justified_epoch);

    if (logger.isDebugEnabled() &&
        !origState.getJustifiedEpoch().equals(state.getJustifiedEpoch()) ||
        !origState.getFinalizedEpoch().equals(state.getFinalizedEpoch())) {
      logger.debug("Justified / Finalized epoch changes " +
          origState.getJustifiedEpoch() + "=>" + state.getJustifiedEpoch() + " / " +
          origState.getFinalizedEpoch() + "=>" + state.getFinalizedEpoch());
    }

    // Crosslinks

    /*
    For every slot in range(get_epoch_start_slot(previous_epoch), get_epoch_start_slot(next_epoch)),
    let crosslink_committees_at_slot = get_crosslink_committees_at_slot(state, slot).
       For every (crosslink_committee, shard) in crosslink_committees_at_slot, compute:
           Set state.latest_crosslinks[shard] = Crosslink(epoch=slot_to_epoch(slot),
              crosslink_data_root=winning_root(crosslink_committee))
           if 3 * total_attesting_balance(crosslink_committee) >= 2 * get_total_balance(crosslink_committee).
    */
    for (SlotNumber slot : spec.get_epoch_start_slot(previous_epoch)
            .iterateTo(spec.get_epoch_start_slot(next_epoch))) {
      List<ShardCommittee> crosslink_committees_at_slot = spec
          .get_crosslink_committees_at_slot(state, slot);
      for (ShardCommittee committee : crosslink_committees_at_slot) {
        List<ValidatorIndex> crosslink_committee = committee.getCommittee();
        ShardNumber shard = committee.getShard();
        if (total_attesting_balance.apply(crosslink_committee).times(3).greaterEqual(
            spec.get_total_balance(state, crosslink_committee).times(2))) {
          state.getLatestCrosslinks().set(shard,
              new Crosslink(spec.slot_to_epoch(slot), winning_root.get(crosslink_committee)));
        }
      }
    }

    /*
    Rewards and penalties

    First, we define some additional helpers:
    */

    //     Let base_reward_quotient = BASE_REWARD_QUOTIENT *
    //        integer_squareroot(total_balance // GWEI_PER_ETH)
    // Let base_reward_quotient =
    //    integer_squareroot(previous_total_balance) // BASE_REWARD_QUOTIENT.
    Gwei base_reward_quotient = Gwei.castFrom(
            spec.integer_squareroot(previous_total_balance)
                .dividedBy(spec.getConstants().getBaseRewardQuotient()));

    // Let base_reward(state, index) = get_effective_balance(state, index) //
    //    base_reward_quotient // 5 for any validator with the given index
    Function<ValidatorIndex, Gwei> base_reward = index ->
        spec.get_effective_balance(state, index)
            .dividedBy(base_reward_quotient)
            .dividedBy(5);

    // Let inactivity_penalty(state, index, epochs_since_finality) = base_reward(state, index) +
    // get_effective_balance(state, index) * epochs_since_finality //
    // INACTIVITY_PENALTY_QUOTIENT // 2 for any validator with the given index
    BiFunction<ValidatorIndex, EpochNumber, Gwei> inactivity_penalty =
        (index, epochs_since_finality) ->
        base_reward.apply(index).plus(
            spec.get_effective_balance(state, index)
                .times(epochs_since_finality)
                .dividedBy(spec.getConstants().getInactivityPenaltyQuotient())
                .dividedBy(2));

    /*
     Justification and finalization
    */

    // Note: When applying penalties in the following balance recalculations
    // implementers should make sure the uint64 does not underflow.
    // Note: Rewards and penalties are for participation in the previous epoch,
    // so the "active validator" set is drawn from
    // get_active_validator_indices(state.validator_registry, previous_epoch).

    // Let epochs_since_finality = next_epoch - state.finalized_epoch.
    EpochNumber epochs_since_finality = next_epoch.minus(state.getFinalizedEpoch());

    if (epochs_since_finality.lessEqual(EpochNumber.of(4))) {
      // Case 1: epochs_since_finality <= 4:
      logger.debug("Case 1: epochs_since_finality <= 4");
      if (summary != null) {
        summary.noFinality = false;
      }

      //  Expected FFG source:

      //  Any validator index in previous_epoch_attester_indices gains base_reward(state, index) *
      //      previous_epoch_attesting_balance // previous_total_balance.
      for (ValidatorIndex index : previous_epoch_attester_indices) {
        Gwei reward = base_reward.apply(index)
            .times(previous_epoch_attesting_balance)
            .dividedBy(previous_total_balance);
        state.getValidatorBalances().update(index, balance ->
            balance.plus(reward));

        if (summary != null) {
          summary.attestationRewards.put(index, reward);
        }
      }
      if (logger.isTraceEnabled() && !previous_epoch_attester_indices.isEmpty()) {
        logger.trace("Rewarded: Previous epoch justified attesters: "
            + previous_epoch_attester_indices);
      }

      //  Any active validator index not in previous_epoch_attester_indices loses base_reward(state, index).
      //  FIXME 'active validator' - not exact meaning
      List<ValidatorIndex> previous_epoch_justified_attester_loosers = new ArrayList<>();
      for (ValidatorIndex index : previous_active_validator_indices) {
        if (!previous_epoch_attester_indices.contains(index)) {
          Gwei penalty = base_reward.apply(index);
          state.getValidatorBalances().update(index, balance -> balance.minus(penalty));

          previous_epoch_justified_attester_loosers.add(index);
          if (summary != null) {
            summary.attestationPenalties.put(index, penalty);
          }
        }
      }
      if (logger.isDebugEnabled() && !previous_epoch_justified_attester_loosers.isEmpty()) {
        logger.debug("Penalized: Previous epoch justified attesters: "
            + previous_epoch_justified_attester_loosers);
      }

      //  Expected FFG target:

      //  Any validator index in previous_epoch_boundary_attester_indices gains
      //    base_reward(state, index) * previous_epoch_boundary_attesting_balance // previous_total_balance.
      for (ValidatorIndex index : previous_epoch_boundary_attester_indices) {
        Gwei reward = base_reward.apply(index)
            .times(previous_epoch_boundary_attesting_balance)
            .dividedBy(previous_total_balance);
        state.getValidatorBalances().update(index, balance -> balance.plus(reward));

        if (summary != null) {
          summary.boundaryAttestationRewards.put(index, reward);
        }
      }
      if (logger.isTraceEnabled() && !previous_epoch_boundary_attester_indices.isEmpty()) {
        logger.trace("Rewarded: Previous epoch boundary attesters: "
            + previous_epoch_boundary_attester_indices);
      }

      //  Any active validator index not in previous_epoch_boundary_attester_indices loses
      //    base_reward(state, index).
      //  FIXME 'active validator' - not exact meaning
      List<ValidatorIndex> previous_epoch_boundary_attester_loosers = new ArrayList<>();
      for (ValidatorIndex index : previous_active_validator_indices) {
        if (!previous_epoch_boundary_attester_indices.contains(index)) {
          Gwei penalty = base_reward.apply(index);
          state.getValidatorBalances().update(index, balance ->
              balance.minus(penalty));
          previous_epoch_boundary_attester_loosers.add(index);
          if (summary != null) {
            summary.boundaryAttestationPenalties.put(index, penalty);
          }
        }
      }
      if (logger.isDebugEnabled() && !previous_epoch_boundary_attester_loosers.isEmpty()) {
        logger.debug("Penalized: Previous epoch boundary attesters: "
            + previous_epoch_boundary_attester_loosers);
      }

      //  Expected beacon chain head:

      //  Any validator index in previous_epoch_head_attester_indices gains
      //    base_reward(state, index) * previous_epoch_head_attesting_balance // previous_total_balance).
      for (ValidatorIndex index : previous_epoch_head_attester_indices) {
        Gwei reward = base_reward.apply(index)
            .times(previous_epoch_head_attesting_balance)
            .dividedBy(previous_total_balance);
        state.getValidatorBalances().update(index, balance -> balance.plus(reward));
        if (summary != null) {
          summary.beaconHeadAttestationRewards.put(index, reward);
        }

      }
      if (logger.isTraceEnabled() && !previous_epoch_head_attester_indices.isEmpty()) {
        logger.trace("Rewarded: Previous epoch head attesters: "
            + previous_epoch_head_attester_indices);
      }

      //  Any active validator index not in previous_epoch_head_attester_indices loses
      //    base_reward(state, index).
      List<ValidatorIndex> previous_epoch_head_attester_loosers = new ArrayList<>();
      for (ValidatorIndex index : previous_active_validator_indices) {
        if (!previous_epoch_head_attester_indices.contains(index)) {
          Gwei penalty = base_reward.apply(index);
          state.getValidatorBalances().update(index, balance ->
              balance.minus(penalty));
          previous_epoch_head_attester_loosers.add(index);
          if (summary != null) {
            summary.beaconHeadAttestationPenalties.put(index, penalty);
          }
        }
      }
      if (logger.isDebugEnabled() && !previous_epoch_head_attester_loosers.isEmpty()) {
        logger.debug("Penalized: Previous epoch head attesters: "
            + previous_epoch_head_attester_loosers);
      }

      //  Inclusion distance:

      // Any validator index in previous_epoch_attester_indices gains
      //    base_reward(state, index) * MIN_ATTESTATION_INCLUSION_DELAY //
      //        inclusion_distance(state, index)
      for (ValidatorIndex index : previous_epoch_attester_indices) {
        Gwei reward = base_reward.apply(index)
            .times(spec.getConstants().getMinAttestationInclusionDelay())
            .dividedBy(inclusion_distance.get(index));
        state.getValidatorBalances().update(index, balance -> balance.plus(reward));
        if (summary != null) {
          summary.inclusionDistanceRewards.put(index, reward);
        }
      }
      if (logger.isTraceEnabled() && !previous_epoch_attester_indices.isEmpty()) {
        logger.trace("Rewarded: Previous epoch attesters: " + previous_epoch_attester_indices);
      }
    } else {
      // Case 2: epochs_since_finality > 4:
      logger.debug("Case 2: epochs_since_finality > 4");
      if (summary != null) {
        summary.noFinality = true;
      }

      //  Any active validator index not in previous_epoch_attester_indices, loses
      //      inactivity_penalty(state, index, epochs_since_finality).
      List<ValidatorIndex> previous_epoch_justified_attester_loosers = new ArrayList<>();
      for (ValidatorIndex index : previous_active_validator_indices) {
        if (!previous_epoch_attester_indices.contains(index)) {
          Gwei penalty = inactivity_penalty.apply(index, epochs_since_finality);
          state.getValidatorBalances().update(index, balance -> balance.minus(penalty));
          previous_epoch_justified_attester_loosers.add(index);
          if (summary != null) {
            summary.attestationPenalties.put(index, penalty);
          }
        }
      }
      if (logger.isDebugEnabled() && !previous_epoch_justified_attester_loosers.isEmpty()) {
        logger.debug("Penalized: Previous epoch justified attesters: "
            + previous_epoch_justified_attester_loosers);
      }

      //  Any active validator index not in previous_epoch_boundary_attester_indices, loses
      //      inactivity_penalty(state, index, epochs_since_finality).
      List<ValidatorIndex> previous_epoch_boundary_attester_loosers = new ArrayList<>();
      for (ValidatorIndex index : previous_active_validator_indices) {
        if (!previous_epoch_boundary_attester_indices.contains(index)) {
          Gwei penalty = inactivity_penalty.apply(index, epochs_since_finality);
          state.getValidatorBalances().update(index, balance ->
              balance.minus(penalty));
          previous_epoch_boundary_attester_loosers.add(index);
          if (summary != null) {
            summary.boundaryAttestationPenalties.put(index, penalty);
          }
        }
      }
      if (logger.isDebugEnabled() && !previous_epoch_boundary_attester_loosers.isEmpty()) {
        logger.debug("Penalized: Previous epoch boundary attesters: "
            + previous_epoch_boundary_attester_loosers);
      }

      //  Any active validator index not in previous_epoch_head_attester_indices, loses
      //      base_reward(state, index).
      List<ValidatorIndex> previous_epoch_head_attester_loosers = new ArrayList<>();
      for (ValidatorIndex index : previous_active_validator_indices) {
        if (!previous_epoch_head_attester_indices.contains(index)) {
          Gwei penalty = base_reward.apply(index);
          state.getValidatorBalances().update(index, balance ->
              balance.minus(penalty));
          previous_epoch_head_attester_loosers.add(index);
          if (summary != null) {
            summary.beaconHeadAttestationPenalties.put(index, penalty);
          }
        }
      }
      if (logger.isDebugEnabled() && !previous_epoch_head_attester_loosers.isEmpty()) {
        logger.debug("Penalized: Previous epoch head attesters: " + previous_epoch_head_attester_loosers);
      }

      //  Any active validator index with validator.slashed == True,
      //  loses 2 * inactivity_penalty(state, index, epochs_since_finality) + base_reward(state, index).
      List<ValidatorIndex> inactive_attester_loosers = new ArrayList<>();
      for (ValidatorIndex index : previous_active_validator_indices) {
        ValidatorRecord validator = state.getValidatorRegistry().get(index);
        Gwei penalty = inactivity_penalty.apply(index, epochs_since_finality)
            .times(2).plus(base_reward.apply(index));
        if (validator.getSlashed()) {
          state.getValidatorBalances().update(index, balance -> balance.minus(penalty));
          inactive_attester_loosers.add(index);
          if (summary != null) {
            summary.initiatedExitPenalties.put(index, penalty);
          }
        }
      }
      if (logger.isDebugEnabled() && !inactive_attester_loosers.isEmpty()) {
        logger.debug("Penalized: Inactive attesters: " + inactive_attester_loosers);
      }

      //  Any validator index in previous_epoch_attester_indices loses
      //    base_reward(state, index) - base_reward(state, index) *
      //        MIN_ATTESTATION_INCLUSION_DELAY // inclusion_distance(state, index)
      for (ValidatorIndex index : previous_epoch_attester_indices) {
        Gwei penalty = base_reward.apply(index)
            .minus(
                base_reward.apply(index)
                    .times(spec.getConstants().getMinAttestationInclusionDelay())
                    .dividedBy(inclusion_distance.get(index)));
        state.getValidatorBalances().update(index, balance -> balance.minus(penalty));
        if (summary != null) {
          summary.noFinalityPenalties.put(index, penalty);
        }
      }
      if (logger.isDebugEnabled() && !previous_epoch_attester_indices.isEmpty()) {
        logger.debug("Penalized: No finality attesters: " + previous_epoch_attester_indices);
      }
    }

    /*
    Attestation inclusion
    */

    // For each index in previous_epoch_attester_indices, we determine the proposer
    //    proposer_index = get_beacon_proposer_index(state, inclusion_slot(state, index))
    //    and set state.validator_balances[proposer_index] +=
    //      base_reward(state, index) // ATTESTATION_INCLUSION_REWARD_QUOTIENT.
    Set<ValidatorIndex> attestation_inclusion_gainers = new HashSet<>();
    for (ValidatorIndex index : previous_epoch_attester_indices) {
      ValidatorIndex proposer_index = spec
          .get_beacon_proposer_index(state, inclusion_slot.get(index));
      Gwei reward = base_reward.apply(index).dividedBy(spec.getConstants().getAttestationInclusionRewardQuotient());
      state.getValidatorBalances().update(proposer_index, balance ->
          balance.plus(reward));
      attestation_inclusion_gainers.add(proposer_index);
      if (summary != null) {
        summary.attestationInclusionRewards.put(proposer_index, reward);
      }


    }
    if (logger.isTraceEnabled() && !attestation_inclusion_gainers.isEmpty()) {
      logger.trace("Rewarded: Attestation include proposers: " + attestation_inclusion_gainers);
    }

    /*
    Crosslinks

    For every slot in range(get_epoch_start_slot(previous_epoch), get_epoch_start_slot(current_epoch)):
       Let crosslink_committees_at_slot = get_crosslink_committees_at_slot(state, slot).
       For every (crosslink_committee, shard) in crosslink_committees_at_slot and every index in crosslink_committee:
           If index in attesting_validators(crosslink_committee),
               state.validator_balances[index] += base_reward(state, index) *
                   total_attesting_balance(crosslink_committee) // get_total_balance(state, crosslink_committee)).
           If index not in attesting_validators(crosslink_committee),
               state.validator_balances[index] -= base_reward(state, index).
    */
    Set<ValidatorIndex> crosslink_attestation_gainers = new HashSet<>();
    Set<ValidatorIndex> crosslink_attestation_loosers = new HashSet<>();
    for (SlotNumber slot: spec.get_epoch_start_slot(previous_epoch)
            .iterateTo(spec.get_epoch_start_slot(current_epoch))) {
      for (ShardCommittee committee : spec.get_crosslink_committees_at_slot(state, slot)) {
        List<ValidatorIndex> crosslink_committee = committee.getCommittee();
        Set<ValidatorIndex> attesting_validator_set = attesting_validators.apply(crosslink_committee);
        for (ValidatorIndex index : crosslink_committee) {
          if (attesting_validator_set.contains(index)) {
            state.getValidatorBalances().update(index,
                vb -> vb.plus(base_reward.apply(index).mulDiv(total_attesting_balance.apply(crosslink_committee),
                            spec.get_total_balance(state, crosslink_committee))));
            crosslink_attestation_gainers.add(index);
          } else {
            state.getValidatorBalances().update(index, vb -> vb.minus(base_reward.apply(index)));
            crosslink_attestation_loosers.add(index);
          }
        }
      }
    }
    if (logger.isTraceEnabled() && !crosslink_attestation_gainers.isEmpty()) {
      logger.trace("Rewarded: Crosslink attesters: " + crosslink_attestation_gainers);
    }
    if (logger.isDebugEnabled() && !crosslink_attestation_loosers.isEmpty()) {
      logger.debug("Penalized: Crosslink attesters: " + crosslink_attestation_loosers);
    }


    /*
    Ejections

    Run process_ejections(state).

    def process_ejections(state: BeaconState) -> None:
      """
      Iterate through the validator registry
      and eject active validators with balance below ``EJECTION_BALANCE``.
      """
      for index in get_active_validator_indices(state.validator_registry, current_epoch(state)):
          if state.validator_balances[index] < EJECTION_BALANCE:
              exit_validator(state, index)
     */


    Set<ValidatorIndex> exit_validators = new HashSet<>();
    for (ValidatorIndex index : spec.get_active_validator_indices(
            state.getValidatorRegistry(), current_epoch)) {
      if (state.getValidatorBalances().get(index).less(spec.getConstants().getEjectionBalance())) {
        spec.exit_validator(state, index);
        exit_validators.add(index);
        if (summary != null) {
          summary.ejectedValidators.add(index);
        }
      }
    }
    if (logger.isInfoEnabled() && !exit_validators.isEmpty()) {
      logger.info("Validators ejected: " + exit_validators);
    }

    /*
          Validator registry and shuffling seed data
    */

    // First, update the following:

    // Set state.previous_shuffling_epoch = state.current_shuffling_epoch.
    state.setPreviousShufflingEpoch(state.getCurrentShufflingEpoch());
    // Set state.previous_shuffling_start_shard = state.current_shuffling_start_shard.
    state.setPreviousShufflingStartShard(state.getCurrentShufflingStartShard());
    // Set state.previous_shuffling_seed = state.current_shuffling_seed.
    state.setPreviousShufflingSeed(state.getCurrentShufflingSeed());

    /*
      If the following are satisfied:
         state.finalized_epoch > state.validator_registry_update_epoch
         state.latest_crosslinks[shard].epoch > state.validator_registry_update_epoch
             for every shard number shard in
             [(state.current_shuffling_start_shard + i) % SHARD_COUNT
                 for i in range(get_current_epoch_committee_count(state))]
                 (that is, for every shard in the current committees)

    */
    boolean updateRegistry =
        state.getFinalizedEpoch().greater(state.getValidatorRegistryUpdateEpoch());

    for (int i = 0; i < spec.get_current_epoch_committee_count(state); i++) {
      ShardNumber shard = state.getCurrentShufflingStartShard().plusModulo(i, spec.getConstants().getShardCount());
      if (!state.getLatestCrosslinks().get(shard).getEpoch().greater(
          state.getValidatorRegistryUpdateEpoch())) {
        updateRegistry = false;
        break;
      }
    }

    if (updateRegistry) {
      //    update the validator registry and associated fields by running
      spec.update_validator_registry(state);
      // Set state.current_shuffling_epoch = next_epoch
      state.setCurrentShufflingEpoch(next_epoch);
      // Set state.current_shuffling_start_shard = (state.current_shuffling_start_shard +
      //    get_current_epoch_committee_count(state)) % SHARD_COUNT
      state.setCurrentShufflingStartShard(state.getCurrentShufflingStartShard().plusModulo(
          spec.get_current_epoch_committee_count(state), spec.getConstants().getShardCount()));
      // Set state.current_shuffling_seed = generate_seed(state, state.current_shuffling_epoch)
      state.setCurrentShufflingSeed(spec.generate_seed(state, state.getCurrentShufflingEpoch()));

    } else {
      //    If a validator registry update does not happen do the following:

      // Let epochs_since_last_registry_update = current_epoch - state.validator_registry_update_epoch.
      EpochNumber epochs_since_last_registry_update =
          current_epoch.minus(state.getValidatorRegistryUpdateEpoch());

      // If epochs_since_last_registry_update > 1 and is_power_of_two(epochs_since_last_registry_update):
      if (epochs_since_last_registry_update.greater(EpochNumber.of(1)) &&
          epochs_since_last_registry_update.isPowerOf2()) {
        // Set state.current_shuffling_epoch = next_epoch.
        state.setCurrentShufflingEpoch(next_epoch);
        // Set state.current_shuffling_seed = generate_seed(state, state.current_shuffling_epoch)
        state.setCurrentShufflingSeed(spec.generate_seed(state, state.getCurrentShufflingEpoch()));
        // Note that state.current_shuffling_start_shard is left unchanged.
      }
    }

    // Regardless of whether or not a validator set change happens, run the following:
    spec.process_slashings(state);
    spec.process_exit_queue(state);

    /*
     Final updates
    */

    //  Set state.latest_active_index_roots[(next_epoch + ACTIVATION_EXIT_DELAY) % LATEST_ACTIVE_INDEX_ROOTS_LENGTH] =
    //      hash_tree_root(get_active_validator_indices(state, next_epoch + ACTIVATION_EXIT_DELAY)).
    state.getLatestActiveIndexRoots().set(
        next_epoch.plus(spec.getConstants().getActivationExitDelay()).modulo(spec.getConstants().getLatestActiveIndexRootsLength()),
        spec.hash_tree_root(spec.get_active_validator_indices(state.getValidatorRegistry(),
            next_epoch.plus(spec.getConstants().getActivationExitDelay()))));
    //  Set state.latest_slashed_balances[(next_epoch) % LATEST_SLASHED_EXIT_LENGTH] =
    //      state.latest_slashed_balances[current_epoch % LATEST_SLASHED_EXIT_LENGTH].
    state.getLatestSlashedBalances().set(next_epoch.modulo(spec.getConstants().getLatestSlashedExitLength()),
        state.getLatestSlashedBalances().get(
            current_epoch.modulo(spec.getConstants().getLatestSlashedExitLength())));
    //  Set state.latest_randao_mixes[next_epoch % LATEST_RANDAO_MIXES_LENGTH] =
    //      get_randao_mix(state, current_epoch).
    state.getLatestRandaoMixes().set(next_epoch.modulo(spec.getConstants().getLatestRandaoMixesLength()),
        spec.get_randao_mix(state, current_epoch));
    //  Remove any attestation in state.latest_attestations such that
    //      slot_to_epoch(attestation.data.slot) < current_epoch.
    state.getLatestAttestations().remove(
        a -> spec.slot_to_epoch(a.getData().getSlot()).less(current_epoch));

    BeaconStateEx ret = new BeaconStateExImpl(state.createImmutable(),
        origState.getHeadBlockHash(), TransitionType.EPOCH);

    if (summary != null) {
      summary.postState = ret;
    }

    logger.debug(() -> "Epoch transition result state: (" +
        spec.hash_tree_root(ret).toStringShort() + ") " + ret.toString(spec.getConstants()));

    return ret;
  }
}
