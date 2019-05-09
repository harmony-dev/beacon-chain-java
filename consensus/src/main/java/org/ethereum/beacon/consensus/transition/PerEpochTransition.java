package org.ethereum.beacon.consensus.transition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.javatuples.Pair;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

/**
 * Per-epoch transition, which happens at the start of the first slot of every epoch.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.1/specs/core/0_beacon-chain.md#per-epoch-processing">Per-epoch
 *     processing</a> in the spec.
 */
public class PerEpochTransition implements StateTransition<BeaconStateEx> {
  private static final Logger logger = LogManager.getLogger(PerEpochTransition.class);

  private final BeaconChainSpec spec;

  public PerEpochTransition(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx stateEx) {
    return apply(stateEx, null);
  }

  BeaconStateEx apply(BeaconStateEx origState, EpochTransitionSummary summary) {
    logger.debug(() -> "Applying epoch transition to state: (" +
        spec.hash_tree_root(origState).toStringShort() + ") " +
        origState.toString(spec.getConstants(), spec::signing_root));

    TransitionType.EPOCH.checkCanBeAppliedAfter(origState.getTransition());

    if (summary != null) {
      summary.preState = origState;
    }

    MutableBeaconState state = origState.createMutableCopy();

    if (summary != null) {
      summary.currentEpochSummary.activeAttesters =
          spec.get_active_validator_indices(state, spec.get_current_epoch(state));
      summary.currentEpochSummary.validatorBalance =
          spec.get_total_balance(state, summary.currentEpochSummary.activeAttesters);
      List<PendingAttestation> current_epoch_boundary_attestations =
          spec.get_matching_source_attestations(state, spec.get_current_epoch(state));
      summary.currentEpochSummary.boundaryAttesters =
          current_epoch_boundary_attestations.stream()
              .flatMap(
                  a ->
                      spec
                          .get_attesting_indices(
                              state, a.getData(), a.getAggregationBitfield())
                          .stream())
              .collect(Collectors.toList());
      summary.currentEpochSummary.boundaryAttestingBalance =
          spec.get_attesting_balance(state, current_epoch_boundary_attestations);

      summary.previousEpochSummary.activeAttesters =
          spec.get_active_validator_indices(state, spec.get_previous_epoch(state));
      summary.previousEpochSummary.validatorBalance =
          spec.get_total_balance(state, summary.previousEpochSummary.activeAttesters);
      List<PendingAttestation> previous_epoch_boundary_attestations =
          spec.get_matching_source_attestations(state, spec.get_previous_epoch(state));
      summary.previousEpochSummary.boundaryAttesters =
          previous_epoch_boundary_attestations.stream()
              .flatMap(
                  a ->
                      spec
                          .get_attesting_indices(
                              state, a.getData(), a.getAggregationBitfield())
                          .stream())
              .collect(Collectors.toList());
      summary.previousEpochSummary.boundaryAttestingBalance =
          spec.get_attesting_balance(state, previous_epoch_boundary_attestations);
      List<PendingAttestation> previous_epoch_matching_head_attestations =
          spec.get_matching_head_attestations(state, spec.get_previous_epoch(state));
      summary.headAttesters =
          previous_epoch_matching_head_attestations.stream()
              .flatMap(
                  a ->
                      spec
                          .get_attesting_indices(
                              state, a.getData(), a.getAggregationBitfield())
                          .stream())
              .collect(Collectors.toList());
      summary.justifiedAttesters.addAll(summary.previousEpochSummary.activeAttesters);
      summary.justifiedAttestingBalance = summary.previousEpochSummary.validatorBalance;

      EpochNumber epochs_since_finality =
          spec.get_current_epoch(state).increment().minus(state.getFinalizedEpoch());

      if (epochs_since_finality.lessEqual(spec.getConstants().getMinEpochsToInactivityPenalty())) {
        summary.noFinality = false;
      } else {
        summary.noFinality = true;
      }

      EpochNumber previous_epoch = spec.get_previous_epoch(state);
      Gwei total_balance = spec.get_total_active_balance(state);

      List<ValidatorIndex> eligible_validator_indices = new ArrayList<>();
      for (ValidatorIndex index : state.getValidatorRegistry().size()) {
        ValidatorRecord validator = state.getValidatorRegistry().get(index);
        if (spec.is_active_validator(validator, previous_epoch)
            && (validator.getSlashed() && previous_epoch.increment().less(validator.getWithdrawableEpoch()))) {
          eligible_validator_indices.add(index);
        }
      }

      List<PendingAttestation> matching_source_attestations =
          spec.get_matching_source_attestations(state, previous_epoch);
      List<PendingAttestation> matching_target_attestations =
          spec.get_matching_target_attestations(state, previous_epoch);
      List<PendingAttestation> matching_head_attestations =
          spec.get_matching_head_attestations(state, previous_epoch);

      // attestation source rewards/penalties
      {
        List<ValidatorIndex> unslashed_attesting_indices =
            spec.get_unslashed_attesting_indices(state, matching_source_attestations);
        Gwei attesting_balance = spec.get_attesting_balance(state, matching_source_attestations);
        for (ValidatorIndex index : eligible_validator_indices) {
          if (unslashed_attesting_indices.contains(index)) {
            summary.attestationRewards.put(index,
                spec.get_base_reward(state, index).times(attesting_balance).dividedBy(total_balance));
          } else {
            summary.attestationPenalties.put(index, spec.get_base_reward(state, index));
          }
        }
      }

      // attestation target rewards/penalties
      {
        List<ValidatorIndex> unslashed_attesting_indices =
            spec.get_unslashed_attesting_indices(state, matching_target_attestations);
        Gwei attesting_balance = spec.get_attesting_balance(state, matching_target_attestations);
        for (ValidatorIndex index : eligible_validator_indices) {
          if (unslashed_attesting_indices.contains(index)) {
            summary.boundaryAttestationRewards.put(index,
                spec.get_base_reward(state, index).times(attesting_balance).dividedBy(total_balance));
          } else {
            summary.boundaryAttestationPenalties.put(index, spec.get_base_reward(state, index));
          }
        }
      }

      // chain head rewards/penalties
      {
        List<ValidatorIndex> unslashed_attesting_indices =
            spec.get_unslashed_attesting_indices(state, matching_head_attestations);
        Gwei attesting_balance = spec.get_attesting_balance(state, matching_head_attestations);
        for (ValidatorIndex index : eligible_validator_indices) {
          if (unslashed_attesting_indices.contains(index)) {
            summary.beaconHeadAttestationRewards.put(index,
                spec.get_base_reward(state, index).times(attesting_balance).dividedBy(total_balance));
          } else {
            summary.beaconHeadAttestationPenalties.put(index, spec.get_base_reward(state, index));
          }
        }
      }

      // inclusion rewards
      for (ValidatorIndex index : spec.get_unslashed_attesting_indices(state, matching_source_attestations)) {
        PendingAttestation attestation =
            matching_source_attestations.stream()
                .filter(a -> spec.get_attesting_indices(state, a.getData(), a.getAggregationBitfield()).contains(index))
                .min(Comparator.comparing(PendingAttestation::getInclusionDelay))
                .get();
        summary.inclusionDistanceRewards.put(index,
            spec.get_base_reward(state, index)
                .times(spec.getConstants().getMinAttestationInclusionDelay())
                .dividedBy(attestation.getInclusionDelay()));
      }

      // inactivity penalty
      EpochNumber finality_delay = previous_epoch.minus(state.getFinalizedEpoch());
      if (finality_delay.greater(spec.getConstants().getMinEpochsToInactivityPenalty())) {
        List<ValidatorIndex> matching_target_attesting_indices =
            spec.get_unslashed_attesting_indices(state, matching_target_attestations);
        for (ValidatorIndex index : eligible_validator_indices) {
          summary.noFinalityPenalties.put(index,
              spec.get_base_reward(state, index).times(spec.getConstants().getBaseRewardsPerEpoch()));
          if (!matching_target_attesting_indices.contains(index)) {
            summary.initiatedExitPenalties.put(index,
                state.getValidatorRegistry().get(index).getEffectiveBalance()
                    .times(finality_delay)
                    .dividedBy(spec.getConstants().getInactivityPenaltyQuotient()));
          }
        }
      }

      EpochNumber epoch = previous_epoch;
      for (UInt64 offset : UInt64s.iterate(UInt64.ZERO, spec.get_epoch_committee_count(state, epoch))) {
        ShardNumber shard = spec.get_epoch_start_shard(state, epoch)
            .plusModulo(offset, spec.getConstants().getShardCount());
        List<ValidatorIndex> crosslink_committee = spec.get_crosslink_committee(state, epoch, shard);
        Pair<Crosslink, List<ValidatorIndex>> winner =
            spec.get_winning_crosslink_and_attesting_indices(state, epoch, shard);
        List<ValidatorIndex> attesting_indices = winner.getValue1();
        Gwei attesting_balance = spec.get_total_balance(state, attesting_indices);
        Gwei committee_balance = spec.get_total_balance(state, crosslink_committee);
        for (ValidatorIndex index : crosslink_committee) {
          Gwei base_reward = spec.get_base_reward(state, index);
          if (attesting_indices.contains(index)) {
            summary.attestationInclusionRewards.put(index,
                base_reward.times(attesting_balance).dividedBy(committee_balance));
          }
        }
      }
    }

    spec.process_justification_and_finalization(state);
    spec.process_crosslinks(state);
    spec.process_rewards_and_penalties(state);
    List<ValidatorIndex> ejectedValidators = spec.process_registry_updates(state);
    spec.process_slashings(state);
    spec.process_final_updates(state);

    BeaconStateEx ret = new BeaconStateExImpl(state.createImmutable(), TransitionType.EPOCH);

    if (summary != null) {
      summary.ejectedValidators = ejectedValidators;
      summary.postState = ret;
    }

    logger.debug(() -> "Epoch transition result state: (" +
        spec.hash_tree_root(ret).toStringShort() + ") " +
        ret.toString(spec.getConstants(), spec::signing_root));

    return ret;
  }
}
