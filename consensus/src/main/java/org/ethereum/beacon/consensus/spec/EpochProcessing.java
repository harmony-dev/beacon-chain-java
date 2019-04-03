package org.ethereum.beacon.consensus.spec;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.HistoricalBatch;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

/**
 * Per epoch processing.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/core/0_beacon-chain.md#per-epoch-processing">Per-epoch
 *     processing</a> in the spec.
 */
public interface EpochProcessing extends HelperFunction {

  /*
    def get_current_total_balance(state: BeaconState) -> Gwei:
      return get_total_balance(state, get_active_validator_indices(state.validator_registry, get_current_epoch(state)))
   */
  default Gwei get_current_total_balance(BeaconState state) {
    return get_total_balance(state,
        get_active_validator_indices(state.getValidatorRegistry(), get_current_epoch(state)));
  }

  /*
    def get_previous_total_balance(state: BeaconState) -> Gwei:
      return get_total_balance(state, get_active_validator_indices(state.validator_registry, get_previous_epoch(state)))
   */
  default Gwei get_previous_total_balance(BeaconState state) {
    return get_total_balance(state,
        get_active_validator_indices(state.getValidatorRegistry(), get_previous_epoch(state)));
  }

  /*
    def get_attesting_indices(state: BeaconState, attestations: List[PendingAttestation]) -> List[ValidatorIndex]:
      output = set()
      for a in attestations:
          output = output.union(get_attestation_participants(state, a.data, a.aggregation_bitfield))
      return sorted(list(output))
   */
  default List<ValidatorIndex> get_attesting_indices(BeaconState state, List<PendingAttestation> attestations) {
    List<ValidatorIndex> output = new ArrayList<>();
    for (PendingAttestation a : attestations) {
      output.addAll(get_attestation_participants(state, a.getData(), a.getAggregationBitfield()));
    }
    Collections.sort(output);
    return output;
  }

  /*
    def get_attesting_balance(state: BeaconState, attestations: List[PendingAttestation]) -> Gwei:
      return get_total_balance(state, get_attesting_indices(state, attestations))
   */
  default Gwei get_attesting_balance(BeaconState state, List<PendingAttestation> attestations) {
    return get_total_balance(state, get_attesting_indices(state, attestations));
  }

  /*
    def get_current_epoch_boundary_attestations(state: BeaconState) -> List[PendingAttestation]:
      return [
          a for a in state.current_epoch_attestations
          if a.data.target_root == get_block_root(state, get_epoch_start_slot(get_current_epoch(state)))
      ]
   */
  default List<PendingAttestation> get_current_epoch_boundary_attestations(BeaconState state) {
    return state.getCurrentEpochAttestations().stream()
        .filter(a -> a.getData()
            .getTargetRoot()
            .equals(get_block_root(state, get_epoch_start_slot(get_current_epoch(state)))))
        .collect(toList());
  }

  /*
    def get_previous_epoch_boundary_attestations(state: BeaconState) -> List[PendingAttestation]:
      return [
          a for a in state.previous_epoch_attestations
          if a.data.target_root == get_block_root(state, get_epoch_start_slot(get_previous_epoch(state)))
      ]
   */
  default List<PendingAttestation> get_previous_epoch_boundary_attestations(BeaconState state) {
    return state.getPreviousEpochAttestations().stream()
        .filter(a -> a.getData()
            .getTargetRoot()
            .equals(get_block_root(state, get_epoch_start_slot(get_previous_epoch(state)))))
        .collect(toList());
  }

  /*
    def get_previous_epoch_matching_head_attestations(state: BeaconState) -> List[PendingAttestation]:
      return [
          a for a in state.previous_epoch_attestations
          if a.data.beacon_block_root == get_block_root(state, a.data.slot)
      ]
   */
  default List<PendingAttestation> get_previous_epoch_matching_head_attestations(BeaconState state) {
    return state.getPreviousEpochAttestations().stream()
        .filter(a -> a.getData()
            .getBeaconBlockRoot()
            .equals(get_block_root(state, a.getData().getSlot())))
        .collect(toList());
  }

  /*
    def get_attestations_for(root) -> List[PendingAttestation]:
        return [a for a in valid_attestations if a.data.crosslink_data_root == root]
   */
  default Pair<Hash32, List<ValidatorIndex>> get_winning_root_and_participants(
      BeaconState state, ShardNumber shard, EpochNumber epoch) {
    ReadList<ShardNumber, Crosslink> previous_crosslinks =
        slot_to_epoch(state.getSlot()).equals(epoch) ?
            state.getCurrentEpochCrosslinks() : state.getPreviousEpochCrosslinks();
    ReadList<Integer, PendingAttestation> attestations =
        slot_to_epoch(state.getSlot()).equals(epoch) ?
            state.getCurrentEpochAttestations() : state.getPreviousEpochAttestations();

    List<PendingAttestation> valid_attestations =
        attestations.stream()
            .filter(a -> a.getData().getShard().equals(shard))
            .filter(a -> a.getData().getPreviousCrosslink().equals(previous_crosslinks.get(shard)))
            .collect(toList());
    List<Hash32> all_roots =
        valid_attestations.stream().map(a -> a.getData().getCrosslinkDataRoot()).collect(toList());

    // handle when no attestations for shard available
    if (all_roots.isEmpty())
      return Pair.with(Hash32.ZERO, emptyList());

    /*
      def get_attestations_for(root) -> List[PendingAttestation]:
        return [a for a in valid_attestations if a.data.crosslink_data_root == root]
     */

    // Winning crosslink root is the root with the most votes for it, ties broken in favor of
    // lexicographically higher hash
    // winning_root = max(all_roots, key=lambda r: (get_attesting_balance(state, get_attestations_for(r)), r))
    Hash32 winning_root = all_roots.stream().max((r1, r2) -> {
      Gwei balance_r1 = get_attesting_balance(state, valid_attestations.stream()
          .filter(a -> a.getData().getCrosslinkDataRoot().equals(r1))
          .collect(toList()));

      Gwei balance_r2 = get_attesting_balance(state, valid_attestations.stream()
          .filter(a -> a.getData().getCrosslinkDataRoot().equals(r1))
          .collect(toList()));

      if (balance_r1.equals(balance_r2)) {
        return r1.toString().compareTo(r2.toString());
      } else {
        return balance_r1.compareTo(balance_r2);
      }
    }).get();

    /*
      return winning_root, get_attesting_indices(state, get_attestations_for(winning_root))
    */
    return Pair.with(
        winning_root,
        get_attesting_indices(state, valid_attestations.stream()
            .filter(a -> a.getData().getCrosslinkDataRoot().equals(winning_root))
            .collect(toList())));
  }

  /*
    def earliest_attestation(state: BeaconState, validator_index: ValidatorIndex) -> PendingAttestation:
      return min([
          a for a in state.previous_epoch_attestations if
          validator_index in get_attestation_participants(state, a.data, a.aggregation_bitfield)
      ], key=lambda a: a.inclusion_slot)
   */
  default PendingAttestation earliest_attestation(BeaconState state, ValidatorIndex validatorIndex) {
    return state.getPreviousEpochAttestations().stream()
        .filter(a -> get_attestation_participants(state, a.getData(), a.getAggregationBitfield())
            .contains(validatorIndex))
        .min(Comparator.comparing(PendingAttestation::getInclusionSlot))
        .get();
  }

  /*
    def inclusion_slot(state: BeaconState, validator_index: ValidatorIndex) -> Slot:
      return earliest_attestation(state, validator_index).inclusion_slot
   */
  default SlotNumber inclusion_slot(BeaconState state, ValidatorIndex validatorIndex) {
    return earliest_attestation(state, validatorIndex).getInclusionSlot();
  }

  /*
    def inclusion_distance(state: BeaconState, validator_index: ValidatorIndex) -> int:
      attestation = earliest_attestation(state, validator_index)
      return attestation.inclusion_slot - attestation.data.slot
   */
  default SlotNumber inclusion_distance(BeaconState state, ValidatorIndex validatorIndex) {
    PendingAttestation attestation = earliest_attestation(state, validatorIndex);
    return attestation.getInclusionSlot().minus(attestation.getData().getSlot());
  }

  /*
    Note: this function mutates beacon state
   */
  default void update_justification_and_finalization(MutableBeaconState state) {
    /*
      new_justified_epoch = state.current_justified_epoch
      new_finalized_epoch = state.finalized_epoch
     */
    EpochNumber new_justified_epoch = state.getCurrentJustifiedEpoch();
    EpochNumber new_finalized_epoch = state.getFinalizedEpoch();

    // Rotate the justification bitfield up one epoch to make room for the current epoch
    state.setJustificationBitfield(state.getJustificationBitfield().shl(1));

    /*
      # If the previous epoch gets justified, fill the second last bit

      previous_boundary_attesting_balance = get_attesting_balance(state, get_previous_epoch_boundary_attestations(state))
      if previous_boundary_attesting_balance * 3 >= get_previous_total_balance(state) * 2:
        new_justified_epoch = get_current_epoch(state) - 1
        state.justification_bitfield |= 2
     */
    Gwei previous_boundary_attesting_balance = get_attesting_balance(state,
        get_previous_epoch_boundary_attestations(state));
    if (previous_boundary_attesting_balance.times(3)
        .greaterEqual(get_previous_total_balance(state).times(2))) {
      new_justified_epoch = get_current_epoch(state).decrement();
      state.setJustificationBitfield(state.getJustificationBitfield().or(2));
    }

    /*
      # If the current epoch gets justified, fill the last bit

      current_boundary_attesting_balance = get_attesting_balance(state, get_current_epoch_boundary_attestations(state))
      if current_boundary_attesting_balance * 3 >= get_current_total_balance(state) * 2:
        new_justified_epoch = get_current_epoch(state)
        state.justification_bitfield |= 1
     */
    Gwei current_boundary_attesting_balance =
        get_attesting_balance(state, get_current_epoch_boundary_attestations(state));
    if (current_boundary_attesting_balance.times(3).greaterEqual(get_current_total_balance(state).times(2))) {
      new_justified_epoch = get_current_epoch(state);
      state.setJustificationBitfield(state.getJustificationBitfield().or(1));
    }

    // Process finalizations

    /*
      bitfield = state.justification_bitfield
      current_epoch = get_current_epoch(state)
     */
    Bitfield64 bitfield = state.getJustificationBitfield();
    EpochNumber current_epoch = get_current_epoch(state);

    /*
      # The 2nd/3rd/4th most recent epochs are all justified, the 2nd using the 4th as source
      if (bitfield >> 1) % 8 == 0b111 and state.previous_justified_epoch == current_epoch - 3:
        new_finalized_epoch = state.previous_justified_epoch */
    if (((bitfield.getValue() >>> 1) % 8 == 0b111L)
        && (state.getPreviousJustifiedEpoch().equals(current_epoch.minus(3)))) {
      new_finalized_epoch = state.getPreviousJustifiedEpoch();
    }

    /*
      # The 2nd/3rd most recent epochs are both justified, the 2nd using the 3rd as source
      if (bitfield >> 1) % 4 == 0b11 and state.previous_justified_epoch == current_epoch - 2:
        new_finalized_epoch = state.previous_justified_epoch */
    if (((bitfield.getValue() >>> 1) % 4 == 0b11L)
        && (state.getPreviousJustifiedEpoch().equals(current_epoch.minus(2)))) {
      new_finalized_epoch = state.getPreviousJustifiedEpoch();
    }

    /*
      # The 1st/2nd/3rd most recent epochs are all justified, the 1st using the 3rd as source
      if (bitfield >> 0) % 8 == 0b111 and state.current_justified_epoch == current_epoch - 2:
          new_finalized_epoch = state.current_justified_epoch */
    if (((bitfield.getValue() >>> 0) % 8 == 0b111L)
        && (state.getCurrentJustifiedEpoch().equals(current_epoch.minus(2)))) {
      new_finalized_epoch = state.getCurrentJustifiedEpoch();
    }

    /*
      # The 1st/2nd most recent epochs are both justified, the 1st using the 2nd as source
      if (bitfield >> 0) % 4 == 0b11 and state.current_justified_epoch == current_epoch - 1:
          new_finalized_epoch = state.current_justified_epoch */
    if (((bitfield.getValue() >>> 0) % 4 == 0b11L)
        && (state.getCurrentJustifiedEpoch().equals(current_epoch.minus(1)))) {
      new_finalized_epoch = state.getCurrentJustifiedEpoch();
    }

    // Update state jusification/finality fields

    /*
      state.previous_justified_epoch = state.current_justified_epoch
      state.previous_justified_root = state.current_justified_root */
    state.setPreviousJustifiedEpoch(state.getCurrentJustifiedEpoch());
    state.setPreviousJustifiedRoot(state.getCurrentJustifiedRoot());

    /*
      if new_justified_epoch != state.current_justified_epoch:
        state.current_justified_epoch = new_justified_epoch
        state.current_justified_root = get_block_root(state, get_epoch_start_slot(new_justified_epoch)) */
    if (!new_justified_epoch.equals(state.getCurrentJustifiedEpoch())) {
      state.setCurrentJustifiedEpoch(new_justified_epoch);
      state.setCurrentJustifiedRoot(get_block_root(state, get_epoch_start_slot(new_justified_epoch)));
    }

    /*
      if new_finalized_epoch != state.finalized_epoch:
        state.finalized_epoch = new_finalized_epoch
        state.finalized_root = get_block_root(state, get_epoch_start_slot(new_finalized_epoch)) */
    if (!new_finalized_epoch.equals(state.getFinalizedEpoch())) {
      state.setFinalizedEpoch(new_finalized_epoch);
      state.setFinalizedRoot(get_block_root(state, get_epoch_start_slot(new_finalized_epoch)));
    }
  }

  default List<Crosslink> get_epoch_crosslinks(BeaconState state, EpochNumber epoch) {
    List<Crosslink> epoch_crosslinks = new ArrayList<>(
        nCopies(getConstants().getShardCount().getIntValue(),
            new Crosslink(EpochNumber.ZERO, Hash32.ZERO))
    );

    for (SlotNumber slot : get_epoch_start_slot(epoch)
        .iterateTo(get_epoch_start_slot(epoch.increment()))) {
      List<ShardCommittee> committees_at_slot = get_crosslink_committees_at_slot(state, slot);
      for (ShardCommittee shard_and_committee : committees_at_slot) {
        Pair<Hash32, List<ValidatorIndex>> root_and_participants =
            get_winning_root_and_participants(state, shard_and_committee.getShard(), slot_to_epoch(slot));
        Gwei participating_balance = get_total_balance(state, root_and_participants.getValue1());
        Gwei total_balance = get_total_balance(state, shard_and_committee.getCommittee());

        if (participating_balance.times(3).greaterEqual(total_balance.times(2))) {
          epoch_crosslinks.set(shard_and_committee.getShard().getIntValue(), new Crosslink(
              slot_to_epoch(slot),
              root_and_participants.getValue0()
          ));
        }
      }
    }
    return epoch_crosslinks;
  }

  default List<Crosslink> merge_crosslinks(List<Crosslink> crosslinks_1, List<Crosslink> crosslinks_2) {
    List<Crosslink> merged_crosslinks = new ArrayList<>(crosslinks_1);
    for (int i = 0; i < merged_crosslinks.size(); i++) {
      if (crosslinks_2.get(i).getEpoch().greater(merged_crosslinks.get(i).getEpoch())) {
        merged_crosslinks.set(i, crosslinks_2.get(i));
      }
    }
    return merged_crosslinks;
  }

  default List<Crosslink> get_latest_crosslinks(BeaconState state) {
    List<Crosslink> previous_epoch_crosslinks = get_epoch_crosslinks(state, get_previous_epoch(state));
    List<Crosslink> current_epoch_crosslinks = get_epoch_crosslinks(state, get_current_epoch(state));
    return merge_crosslinks(
        merge_crosslinks(state.getCurrentEpochCrosslinks().listCopy(), previous_epoch_crosslinks),
        current_epoch_crosslinks
    );
  }

  default void apply_crosslinks(MutableBeaconState state, List<Crosslink> latest_crosslinks) {
    state.getPreviousEpochCrosslinks().clear();
    state.getPreviousEpochCrosslinks().addAll(state.getCurrentEpochCrosslinks().listCopy());

    state.getCurrentEpochCrosslinks().clear();
    state.getCurrentEpochCrosslinks().addAll(latest_crosslinks);
  }

  /*
    Note: this function mutates beacon state

    def maybe_reset_eth1_period(state: BeaconState) -> None:
      if (get_current_epoch(state) + 1) % EPOCHS_PER_ETH1_VOTING_PERIOD == 0:
          for eth1_data_vote in state.eth1_data_votes:
              # If a majority of all votes were for a particular eth1_data value,
              # then set that as the new canonical value
              if eth1_data_vote.vote_count * 2 > EPOCHS_PER_ETH1_VOTING_PERIOD * SLOTS_PER_EPOCH:
                  state.latest_eth1_data = eth1_data_vote.eth1_data
          state.eth1_data_votes = []
  */
  default void maybe_reset_eth1_period(MutableBeaconState state) {
    if (get_current_epoch(state).increment().modulo(getConstants().getEpochsPerEth1VotingPeriod())
        .equals(EpochNumber.ZERO)) {
      for (Eth1DataVote eth1_data_vote : state.getEth1DataVotes()) {
        // If a majority of all votes were for a particular eth1_data value,
        // then set that as the new canonical value
        if (eth1_data_vote.getVoteCount().times(2)
            .compareTo(getConstants().getEpochsPerEth1VotingPeriod().times(getConstants().getSlotsPerEpoch())) > 0) {
          state.setLatestEth1Data(eth1_data_vote.getEth1Data());
        }
      }
      state.getEth1DataVotes().clear();
    }
  }

  /*
    def get_base_reward(state: BeaconState, index: ValidatorIndex) -> Gwei:
      if get_previous_total_balance(state) == 0:
          return 0

      adjusted_quotient = integer_squareroot(get_previous_total_balance(state)) // BASE_REWARD_QUOTIENT
      return get_effective_balance(state, index) // adjusted_quotient // 5
   */
  default Gwei get_base_reward(BeaconState state, ValidatorIndex index) {
    if (get_previous_total_balance(state).equals(Gwei.ZERO)) {
      return Gwei.ZERO;
    }

    UInt64 adjusted_quotient = integer_squareroot(
        get_previous_total_balance(state)).dividedBy(getConstants().getBaseRewardQuotient());
    return get_effective_balance(state, index).dividedBy(adjusted_quotient).dividedBy(5);
  }

  /*
    def get_inactivity_penalty(state: BeaconState, index: ValidatorIndex, epochs_since_finality: int) -> Gwei:
      return (
          get_base_reward(state, index) +
          get_effective_balance(state, index) * epochs_since_finality // INACTIVITY_PENALTY_QUOTIENT // 2
      )
   */
  default Gwei get_inactivity_penalty(BeaconState state, ValidatorIndex index, EpochNumber epochsSinceFinality) {
    return get_base_reward(state, index).plus(
        get_effective_balance(state, index)
            .times(epochsSinceFinality).dividedBy(getConstants().getInactivityPenaltyQuotient()).dividedBy(2)
    );
  }

  /*
    When blocks are finalizing normally...

    # deltas[0] for rewards
    # deltas[1] for penalties
   */
  default Gwei[][] compute_normal_justification_and_finalization_deltas(BeaconState state) {
    /*
      deltas = [
        [0 for index in range(len(state.validator_registry))],
        [0 for index in range(len(state.validator_registry))]
      ] */
    Gwei[][] deltas = {
        new Gwei[state.getValidatorRegistry().size().getIntValue()],
        new Gwei[state.getValidatorRegistry().size().getIntValue()]
    };
    Arrays.fill(deltas[0], Gwei.ZERO);
    Arrays.fill(deltas[1], Gwei.ZERO);

    // Some helper variables
    List<PendingAttestation> previous_epoch_attestations =
        state.getPreviousEpochAttestations().listCopy();
    List<PendingAttestation> boundary_attestations = get_previous_epoch_boundary_attestations(state);
    Gwei boundary_attesting_balance = get_attesting_balance(state, boundary_attestations);
    Gwei total_balance = get_previous_total_balance(state);
    Gwei total_attesting_balance = get_attesting_balance(state, previous_epoch_attestations);
    List<PendingAttestation> matching_head_attestations =
        get_previous_epoch_matching_head_attestations(state);
    Gwei matching_head_balance = get_attesting_balance(state, matching_head_attestations);

    // Process rewards or penalties for all validators
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(state.getValidatorRegistry(), get_previous_epoch(state));
    for (ValidatorIndex index : active_validator_indices) {
      int i = index.getIntValue();
      // Expected FFG source

      /* if index in get_attesting_indices(state, state.previous_epoch_attestations):
            deltas[0][index] += get_base_reward(state, index) * total_attesting_balance // total_balance
            # Inclusion speed bonus
            deltas[0][index] += (
                get_base_reward(state, index) * MIN_ATTESTATION_INCLUSION_DELAY //
                inclusion_distance(state, index)
            ) */
      if (get_attesting_indices(state, previous_epoch_attestations).contains(index)) {
        deltas[0][i] = deltas[0][i].plus(
            get_base_reward(state, index).mulDiv(total_attesting_balance, total_balance));
        // Inclusion speed bonus
        deltas[0][i] = deltas[0][i].plus(
            get_base_reward(state, index)
                .mulDiv(Gwei.castFrom(getConstants().getMinAttestationInclusionDelay()),
                    Gwei.castFrom(inclusion_distance(state, index))));
      } else {
        /* else:
             deltas[1][index] += get_base_reward(state, index) */
        deltas[1][i] = deltas[1][i].plus(get_base_reward(state, index));
      }

      // Expected FFG target

      /* if index in get_attesting_indices(state, boundary_attestations):
           deltas[0][index] += get_base_reward(state, index) * boundary_attesting_balance // total_balance
         else:
           deltas[1][index] += get_base_reward(state, index) */
      if (get_attesting_indices(state, boundary_attestations).contains(index)) {
        deltas[0][i] = deltas[0][i].plus(
            get_base_reward(state, index).mulDiv(boundary_attesting_balance, total_balance));
      } else {
        deltas[1][i] = deltas[1][i].plus(get_base_reward(state, index));
      }

      // Expected head

      /* if index in get_attesting_indices(state, matching_head_attestations):
           deltas[0][index] += get_base_reward(state, index) * matching_head_balance // total_balance
         else:
           deltas[1][index] += get_base_reward(state, index) */
      if (get_attesting_indices(state, matching_head_attestations).contains(index)) {
        deltas[0][i] = deltas[0][i].plus(
            get_base_reward(state, index).mulDiv(matching_head_balance, total_balance));
      } else {
        deltas[1][i] = deltas[1][i].plus(get_base_reward(state, index));
      }

      // Proposer bonus
      /* if index in get_attesting_indices(state, state.previous_epoch_attestations):
            proposer_index = get_beacon_proposer_index(state, inclusion_slot(state, index))
            deltas[0][proposer_index] += get_base_reward(state, index) // ATTESTATION_INCLUSION_REWARD_QUOTIENT */
      if (get_attesting_indices(state, previous_epoch_attestations).contains(index)) {
        ValidatorIndex proposer_index = get_beacon_proposer_index(state, inclusion_slot(state, index));
        deltas[0][proposer_index.getIntValue()] = deltas[0][proposer_index.getIntValue()].plus(
            get_base_reward(state, index).dividedBy(getConstants().getAttestationInclusionRewardQuotient()));
      }
    }

    return deltas;
  }

  /*
    When blocks are not finalizing normally...

    # deltas[0] for rewards
    # deltas[1] for penalties
   */
  default Gwei[][] compute_inactivity_leak_deltas(BeaconState state) {
    /*
      deltas = [
        [0 for index in range(len(state.validator_registry))],
        [0 for index in range(len(state.validator_registry))]
      ] */
    Gwei[][] deltas = {
        new Gwei[state.getValidatorRegistry().size().getIntValue()],
        new Gwei[state.getValidatorRegistry().size().getIntValue()]
    };
    Arrays.fill(deltas[0], Gwei.ZERO);
    Arrays.fill(deltas[1], Gwei.ZERO);

    List<PendingAttestation> previous_epoch_attestations =
        state.getPreviousEpochAttestations().listCopy();
    List<PendingAttestation> boundary_attestations =
        get_previous_epoch_boundary_attestations(state);
    List<PendingAttestation> matching_head_attestations =
        get_previous_epoch_matching_head_attestations(state);
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(state.getValidatorRegistry(), get_previous_epoch(state));
    EpochNumber epochs_since_finality =
        get_current_epoch(state).increment().minus(state.getFinalizedEpoch());

    // for index in active_validator_indices:
    for (ValidatorIndex index : active_validator_indices) {
      int i = index.getIntValue();

      /* if index not in get_attesting_indices(state, state.previous_epoch_attestations):
            deltas[1][index] += get_inactivity_penalty(state, index, epochs_since_finality)
        else:
            # If a validator did attest, apply a small penalty for getting attestations included late
            deltas[0][index] += (
                get_base_reward(state, index) * MIN_ATTESTATION_INCLUSION_DELAY //
                inclusion_distance(state, index)
            )
            deltas[1][index] += get_base_reward(state, index) */
      if (!get_attesting_indices(state, previous_epoch_attestations).contains(index)) {
        deltas[1][i] = deltas[1][i].plus(
            get_inactivity_penalty(state, index, epochs_since_finality));
      } else {
        // If a validator did attest, apply a small penalty for getting attestations included late
        deltas[0][i] = deltas[0][i].plus(
            get_base_reward(state, index).mulDiv(
                Gwei.castFrom(getConstants().getMinAttestationInclusionDelay()),
                Gwei.castFrom(inclusion_distance(state, index))));
        deltas[1][i] = deltas[1][i].plus(get_base_reward(state, index));
      }

      /* if index not in get_attesting_indices(state, boundary_attestations):
            deltas[1][index] += get_inactivity_penalty(state, index, epochs_since_finality) */
      if (!get_attesting_indices(state, boundary_attestations).contains(index)) {
        deltas[1][i] = deltas[1][i].plus(get_inactivity_penalty(state, index, epochs_since_finality));
      }
      /* if index not in get_attesting_indices(state, matching_head_attestations):
            deltas[1][index] += get_base_reward(state, index) */
      if (!get_attesting_indices(state, matching_head_attestations).contains(index)) {
        deltas[1][i] = deltas[1][i].plus(get_base_reward(state, index));
      }
    }

    // Penalize slashed-but-inactive validators as though they were active but offline

    // for index in range(len(state.validator_registry)):
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      /* eligible = (
            index not in active_validator_indices and
            state.validator_registry[index].slashed and
            get_current_epoch(state) < state.validator_registry[index].withdrawable_epoch
        ) */
      boolean eligible = !active_validator_indices.contains(index) &&
          state.getValidatorRegistry().get(index).getSlashed() &&
          get_current_epoch(state).less(state.getValidatorRegistry().get(index).getWithdrawableEpoch());

      /* if eligible:
            deltas[1][index] += (
                2 * get_inactivity_penalty(state, index, epochs_since_finality) +
                get_base_reward(state, index)
            ) */
      if (eligible) {
        deltas[1][index.getIntValue()] = deltas[1][index.getIntValue()].plus(
            get_inactivity_penalty(state, index, epochs_since_finality).times(2)
                .plus(get_base_reward(state, index)));
      }
    }

    return deltas;
  }

  /*
    def get_justification_and_finalization_deltas(state: BeaconState) -> Tuple[List[Gwei], List[Gwei]]:
      epochs_since_finality = get_current_epoch(state) + 1 - state.finalized_epoch
      if epochs_since_finality <= 4:
          return compute_normal_justification_and_finalization_deltas(state)
      else:
          return compute_inactivity_leak_deltas(state)
   */
  default Gwei[][] get_justification_and_finalization_deltas(BeaconState state) {
    EpochNumber epochs_since_finality =
        get_current_epoch(state).increment().minus(state.getFinalizedEpoch());
    if (epochs_since_finality.lessEqual(EpochNumber.of(4))) {
      return compute_normal_justification_and_finalization_deltas(state);
    } else {
      return compute_inactivity_leak_deltas(state);
    }
  }

  /*
     # deltas[0] for rewards
     # deltas[1] for penalties
   */
  default Gwei[][] get_crosslink_deltas(BeaconState state) {
    /*
      deltas = [
        [0 for index in range(len(state.validator_registry))],
        [0 for index in range(len(state.validator_registry))]
      ] */
    Gwei[][] deltas = {
        new Gwei[state.getValidatorRegistry().size().getIntValue()],
        new Gwei[state.getValidatorRegistry().size().getIntValue()]
    };
    Arrays.fill(deltas[0], Gwei.ZERO);
    Arrays.fill(deltas[1], Gwei.ZERO);

    SlotNumber previous_epoch_start_slot = get_epoch_start_slot(get_previous_epoch(state));
    SlotNumber current_epoch_start_slot = get_epoch_start_slot(get_current_epoch(state));

    /* for slot in range(previous_epoch_start_slot, current_epoch_start_slot):
         for crosslink_committee, shard in get_crosslink_committees_at_slot(state, slot): */
    for (SlotNumber slot : previous_epoch_start_slot.iterateTo(current_epoch_start_slot)) {
      List<ShardCommittee> committees_and_shards = get_crosslink_committees_at_slot(state, slot);
      for (ShardCommittee committee_and_shard : committees_and_shards) {
        List<ValidatorIndex> crosslink_committee = committee_and_shard.getCommittee();
        ShardNumber shard = committee_and_shard.getShard();
        /*  winning_root, participants = get_winning_root_and_participants(state, shard)
            participating_balance = get_total_balance(state, participants)
            total_balance = get_total_balance(state, crosslink_committee) */
        Pair<Hash32, List<ValidatorIndex>> winning_root_and_participants =
            get_winning_root_and_participants(state, shard, slot_to_epoch(slot));
        Gwei participating_balance = get_total_balance(state, winning_root_and_participants.getValue1());
        Gwei total_balance = get_total_balance(state, crosslink_committee);

        /* for index in crosslink_committee:
              if index in participants:
                  deltas[0][index] += get_base_reward(state, index) * participating_balance // total_balance
              else:
                  deltas[1][index] += get_base_reward(state, index) */
        for (ValidatorIndex index : crosslink_committee) {
          if (winning_root_and_participants.getValue1().contains(index)) {
            deltas[0][index.getIntValue()] = deltas[0][index.getIntValue()].plus(
                get_base_reward(state, index).mulDiv(participating_balance, total_balance));
          } else {
            deltas[1][index.getIntValue()] = deltas[1][index.getIntValue()].plus(
                get_base_reward(state, index));
          }
        }
      }
    }

    return deltas;
  }

  /*
    Note: this function mutates beacon state.

    def apply_rewards(state: BeaconState) -> None:
      deltas1 = get_justification_and_finalization_deltas(state)
      deltas2 = get_crosslink_deltas(state)
      for i in range(len(state.validator_registry)):
          state.validator_balances[i] = max(
              0,
              state.validator_balances[i] + deltas1[0][i] + deltas2[0][i] - deltas1[1][i] - deltas2[1][i]
          )
   */
  default void apply_rewards(MutableBeaconState state) {
    Gwei[][] deltas1 = get_justification_and_finalization_deltas(state);
    Gwei[][] deltas2 = get_crosslink_deltas(state);
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      int i = index.getIntValue();
      state.getValidatorBalances().update(index, balance ->
          balance.plus(deltas1[0][i]).plus(deltas2[0][i])
              .minusSat(deltas1[1][i]).minusSat(deltas2[1][i]));
    }
  }

  /*
    def process_ejections(state: BeaconState) -> None:
      """
      Iterate through the validator registry
      and eject active validators with balance below ``EJECTION_BALANCE``.
      """
      for index in get_active_validator_indices(state.validator_registry, get_current_epoch(state)):
          if state.validator_balances[index] < EJECTION_BALANCE:
              exit_validator(state, index)
   */
  default List<ValidatorIndex> process_ejections(MutableBeaconState state) {
    List<ValidatorIndex> ejected = new ArrayList<>();
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(state.getValidatorRegistry(), get_current_epoch(state));
    for (ValidatorIndex index : active_validator_indices) {
      if (state.getValidatorBalances().get(index).less(getConstants().getEjectionBalance())) {
        exit_validator(state, index);
        ejected.add(index);
      }
    }
    return ejected;
  }

  /*
    def should_update_validator_registry(state: BeaconState) -> bool:
    # Must have finalized a new block
    if state.finalized_epoch <= state.validator_registry_update_epoch:
        return False
    # Must have processed new crosslinks on all shards of the current epoch
    shards_to_check = [
        (state.current_shuffling_start_shard + i) % SHARD_COUNT
        for i in range(get_current_epoch_committee_count(state))
    ]
    for shard in shards_to_check:
        if state.latest_crosslinks[shard].epoch <= state.validator_registry_update_epoch:
            return False
    return True
   */
  default boolean should_update_validator_registry(BeaconState state) {
    // Must have finalized a new block
    if (state.getFinalizedEpoch().lessEqual(state.getValidatorRegistryUpdateEpoch())) {
      return false;
    }
    // Must have processed new crosslinks on all shards of the current epoch
    List<ShardNumber> shards_to_check = IntStream.range(0, get_current_epoch_committee_count(state))
        .mapToObj(i -> ShardNumber.of(state.getCurrentShufflingStartShard()
            .plus(i).modulo(getConstants().getShardCount()))).collect(toList());
    for (ShardNumber shard : shards_to_check) {
      if (state.getCurrentEpochCrosslinks().get(shard).getEpoch()
          .lessEqual(state.getValidatorRegistryUpdateEpoch())) {
        return false;
      }
    }

    return true;
  }

  /*
    """
    Update validator registry.
    Note that this function mutates ``state``.
    """
   */
  default void update_validator_registry(MutableBeaconState state) {
    EpochNumber current_epoch = get_current_epoch(state);
    // The active validators
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(state.getValidatorRegistry(), current_epoch);
    // The total effective balance of active validators
    Gwei total_balance = get_total_balance(state, active_validator_indices);

    // The maximum balance churn in Gwei (for deposits and exits separately)
    Gwei max_balance_churn = UInt64s.max(
        getConstants().getMaxDepositAmount(),
        total_balance.dividedBy(getConstants().getMaxBalanceChurnQuotient().times(2))
    );

    // Activate validators within the allowable balance churn

    /*  balance_churn = 0
        for index, validator in enumerate(state.validator_registry):
            if validator.activation_epoch == FAR_FUTURE_EPOCH and state.validator_balances[index] >= MAX_DEPOSIT_AMOUNT:
                # Check the balance churn would be within the allowance
                balance_churn += get_effective_balance(state, index)
                if balance_churn > max_balance_churn:
                    break

                # Activate validator
                activate_validator(state, index, is_genesis=False) */
    Gwei balance_churn = Gwei.ZERO;
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      if (validator.getActivationEpoch().equals(getConstants().getFarFutureEpoch()) &&
          state.getValidatorBalances().get(index).greaterEqual(getConstants().getMaxDepositAmount())) {

        // Check the balance churn would be within the allowance
        balance_churn = balance_churn.plus(get_effective_balance(state, index));
        if (balance_churn.greater(max_balance_churn)) {
          break;
        }

        // Activate validator
        activate_validator(state, index, false);
      }
    }

    // Exit validators within the allowable balance churn

    /*  balance_churn = 0
        for index, validator in enumerate(state.validator_registry):
            if validator.exit_epoch == FAR_FUTURE_EPOCH and validator.initiated_exit:
                # Check the balance churn would be within the allowance
                balance_churn += get_effective_balance(state, index)
                if balance_churn > max_balance_churn:
                    break

                # Exit validator
                exit_validator(state, index) */
    balance_churn = Gwei.ZERO;
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      if (validator.getExitEpoch().equals(getConstants().getFarFutureEpoch()) &&
          validator.getInitiatedExit()) {
        // Check the balance churn would be within the allowance
        balance_churn = balance_churn.plus(get_effective_balance(state, index));
        if (balance_churn.greater(max_balance_churn)) {
          break;
        }

        // Exit validator
        exit_validator(state, index);
      }
    }

    state.setValidatorRegistryUpdateEpoch(current_epoch);
  }

  default void update_registry_and_shuffling_data(MutableBeaconState state) {
    // First set previous shuffling data to current shuffling data
    state.setPreviousShufflingEpoch(state.getCurrentShufflingEpoch());
    state.setPreviousShufflingStartShard(state.getCurrentShufflingStartShard());
    state.setPreviousShufflingSeed(state.getCurrentShufflingSeed());
    EpochNumber current_epoch = get_current_epoch(state);
    EpochNumber next_epoch = current_epoch.increment();

    // Check if we should update, and if so, update
    if (should_update_validator_registry(state)) {
      /* update_validator_registry(state)
        # If we update the registry, update the shuffling data and shards as well
        state.current_shuffling_epoch = next_epoch
        state.current_shuffling_start_shard = (
            state.current_shuffling_start_shard +
            get_current_epoch_committee_count(state)
        ) % SHARD_COUNT
        state.current_shuffling_seed = generate_seed(state, state.current_shuffling_epoch) */
      update_validator_registry(state);
      // If we update the registry, update the shuffling data and shards as well
      state.setCurrentShufflingEpoch(next_epoch);
      state.setCurrentShufflingStartShard(state.getCurrentShufflingStartShard().plusModulo(
          get_current_epoch_committee_count(state), getConstants().getShardCount()));
      state.setCurrentShufflingSeed(generate_seed(state, state.getCurrentShufflingEpoch()));
    } else {
      // If processing at least one crosslink keeps failing, then reshuffle every power of two,
      // but don't update the current_shuffling_start_shard

      /* epochs_since_last_registry_update = current_epoch - state.validator_registry_update_epoch
        if epochs_since_last_registry_update > 1 and is_power_of_two(epochs_since_last_registry_update):
            state.current_shuffling_epoch = next_epoch
            state.current_shuffling_seed = generate_seed(state, state.current_shuffling_epoch) */
      EpochNumber epochs_since_last_registry_update = current_epoch.minus(
          state.getValidatorRegistryUpdateEpoch());
      if (epochs_since_last_registry_update.greater(EpochNumber.of(1)) &&
          is_power_of_two(epochs_since_last_registry_update)) {
        state.setCurrentShufflingEpoch(next_epoch);
        state.setCurrentShufflingSeed(generate_seed(state, state.getCurrentShufflingEpoch()));
      }
    }
  }

  /*
    """
    Process the slashings.
    Note that this function mutates ``state``.
    """
   */
  default void process_slashings(MutableBeaconState state) {
    EpochNumber current_epoch = get_current_epoch(state);
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(state.getValidatorRegistry(), current_epoch);
    Gwei total_balance = get_total_balance(state, active_validator_indices);

    // Compute `total_penalties`
    Gwei total_at_start = state.getLatestSlashedBalances().get(current_epoch.increment()
        .modulo(getConstants().getLatestSlashedExitLength()));
    Gwei total_at_end = state.getLatestSlashedBalances()
        .get(current_epoch.modulo(getConstants().getLatestSlashedExitLength()));
    Gwei total_penalties = total_at_end.minusSat(total_at_start);

    /* for index, validator in enumerate(state.validator_registry):
        if validator.slashed and current_epoch == validator.withdrawable_epoch - LATEST_SLASHED_EXIT_LENGTH // 2:
            penalty = max(
                get_effective_balance(state, index) * min(total_penalties * 3, total_balance) // total_balance,
                get_effective_balance(state, index) // MIN_PENALTY_QUOTIENT
            )
            state.validator_balances[index] -= penalty */

    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      if (validator.getSlashed() &&
          current_epoch.equals(validator.getWithdrawableEpoch()
              .minus(getConstants().getLatestSlashedExitLength().half()))) {
        Gwei effective_balance = get_effective_balance(state, index);
        Gwei penalty = UInt64s.max(
            effective_balance.times(UInt64s.min(total_penalties.times(3), total_balance).dividedBy(total_balance)),
            effective_balance.dividedBy(getConstants().getMinPenaltyQuotient())
        );
        state.getValidatorBalances().update(index, balance -> balance.minusSat(penalty));
      }
    }
  }

  /*
    def eligible(index):
      validator = state.validator_registry[index]
      # Filter out dequeued validators
      if validator.withdrawable_epoch != FAR_FUTURE_EPOCH:
          return False
      # Dequeue if the minimum amount of time has passed
      else:
          return get_current_epoch(state) >= validator.exit_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY
   */
  default boolean eligible(BeaconState state, ValidatorIndex index) {
    ValidatorRecord validator = state.getValidatorRegistry().get(index);
    // Filter out dequeued validators
    if (!validator.getWithdrawableEpoch().equals(getConstants().getFarFutureEpoch())) {
      return false;
    } else if (validator.getExitEpoch().equals(getConstants().getFarFutureEpoch())) {
      return false;
    } else {
      // Dequeue if the minimum amount of time has passed
      return get_current_epoch(state).greaterEqual(
          validator.getExitEpoch().plus(getConstants().getMinValidatorWithdrawabilityDelay()));
    }
  }

  /*
    """
    Process the exit queue.
    Note that this function mutates ``state``.
    """
   */
  default void process_exit_queue(MutableBeaconState state) {
    // eligible_indices = filter(eligible, list(range(len(state.validator_registry))))
    // Sort in order of exit epoch,
    // and validators that exit within the same epoch exit in order of validator index
    List<ValidatorIndex> sorted_eligible_indices =
        StreamSupport.stream(state.getValidatorRegistry().size().spliterator(), false)
            .filter(index -> eligible(state, index))
            .sorted(Comparator.comparing(index -> state.getValidatorRegistry().get(index).getExitEpoch()))
            .collect(toList());

    /* for dequeues, index in enumerate(sorted_indices):
        if dequeues >= MAX_EXIT_DEQUEUES_PER_EPOCH:
            break
        prepare_validator_for_withdrawal(state, index) */
    for (int i = 0; i < sorted_eligible_indices.size(); i++) {
      int dequeues = i;
      if (dequeues >= getConstants().getMaxExitDequesPerEpoch().getIntValue()) {
        break;
      }
      prepare_validator_for_withdrawal(state, sorted_eligible_indices.get(i));
    }
  }

  default void finish_epoch_update(MutableBeaconState state) {
    EpochNumber current_epoch = get_current_epoch(state);
    EpochNumber next_epoch = current_epoch.increment();

    // Set active index root
    EpochNumber index_root_position = next_epoch
        .plus(getConstants().getActivationExitDelay()).modulo(getConstants().getLatestActiveIndexRootsLength());
    state.getLatestActiveIndexRoots().set(index_root_position, hash_tree_root(
        get_active_validator_indices(state.getValidatorRegistry(),
            next_epoch.plus(getConstants().getActivationExitDelay()))));

    // Set total slashed balances
    state.getLatestSlashedBalances().set(next_epoch.modulo(getConstants().getLatestSlashedExitLength()),
        state.getLatestSlashedBalances().get(
            current_epoch.modulo(getConstants().getLatestSlashedExitLength())));

    // Set randao mix
    state.getLatestRandaoMixes().set(next_epoch.modulo(getConstants().getLatestRandaoMixesLength()),
        get_randao_mix(state, current_epoch));

    // Set historical root accumulator
    if (next_epoch.modulo(getConstants().getSlotsPerHistoricalRoot().dividedBy(getConstants().getSlotsPerEpoch()))
        .equals(EpochNumber.ZERO)) {
      HistoricalBatch historical_batch =
          new HistoricalBatch(
              state.getLatestBlockRoots().listCopy(),
              state.getLatestStateRoots().listCopy());
      state.getHistoricalRoots().add(hash_tree_root(historical_batch));
    }

    // Rotate current/previous epoch attestations
    state.getPreviousEpochAttestations().clear();
    state.getPreviousEpochAttestations().addAll(state.getCurrentEpochAttestations().listCopy());
    state.getCurrentEpochAttestations().clear();
  }
}
