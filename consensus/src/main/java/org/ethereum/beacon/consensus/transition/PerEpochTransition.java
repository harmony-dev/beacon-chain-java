package org.ethereum.beacon.consensus.transition;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Per-epoch transition, which happens at the start of the first slot of every epoch.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/core/0_beacon-chain.md#per-epoch-processing">Per-epoch
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

  public EpochTransitionSummary applyWithSummary(BeaconStateEx stateEx) {
    EpochTransitionSummary summary = new EpochTransitionSummary();
    apply(stateEx, summary);
    return summary;
  }

  BeaconStateEx apply(BeaconStateEx origState, EpochTransitionSummary summary) {
    logger.debug(() -> "Applying epoch transition to state: (" +
        spec.hash_tree_root(origState).toStringShort() + ") " + origState.toString(spec.getConstants()));

    TransitionType.EPOCH.checkCanBeAppliedAfter(origState.getTransition());

    if (summary != null) {
      summary.preState = origState;
    }

    MutableBeaconState state = origState.createMutableCopy();

    if (summary != null) {
      summary.currentEpochSummary.activeAttesters =
          spec.get_active_validator_indices(
              state.getValidatorRegistry(), spec.get_current_epoch(state));
      summary.currentEpochSummary.validatorBalance =
          spec.get_total_balance(state, summary.currentEpochSummary.activeAttesters);
      List<PendingAttestation> current_epoch_boundary_attestations =
          spec.get_current_epoch_boundary_attestations(state);
      summary.currentEpochSummary.boundaryAttesters =
          current_epoch_boundary_attestations.stream()
              .flatMap(
                  a ->
                      spec
                          .get_attestation_participants(
                              state, a.getData(), a.getAggregationBitfield())
                          .stream())
              .collect(Collectors.toList());
      summary.currentEpochSummary.boundaryAttestingBalance =
          spec.get_attesting_balance(state, current_epoch_boundary_attestations);

      summary.previousEpochSummary.activeAttesters =
          spec.get_active_validator_indices(
              state.getValidatorRegistry(), spec.get_previous_epoch(state));
      summary.previousEpochSummary.validatorBalance =
          spec.get_total_balance(state, summary.previousEpochSummary.activeAttesters);
      List<PendingAttestation> previous_epoch_boundary_attestations =
          spec.get_previous_epoch_boundary_attestations(state);
      summary.previousEpochSummary.boundaryAttesters =
          previous_epoch_boundary_attestations.stream()
              .flatMap(
                  a ->
                      spec
                          .get_attestation_participants(
                              state, a.getData(), a.getAggregationBitfield())
                          .stream())
              .collect(Collectors.toList());
      summary.previousEpochSummary.boundaryAttestingBalance =
          spec.get_attesting_balance(state, previous_epoch_boundary_attestations);
      List<PendingAttestation> previous_epoch_matching_head_attestations =
          spec.get_previous_epoch_matching_head_attestations(state);
      summary.headAttesters =
          previous_epoch_matching_head_attestations.stream()
              .flatMap(
                  a ->
                      spec
                          .get_attestation_participants(
                              state, a.getData(), a.getAggregationBitfield())
                          .stream())
              .collect(Collectors.toList());
      summary.justifiedAttesters.addAll(summary.previousEpochSummary.activeAttesters);
      summary.justifiedAttestingBalance = summary.previousEpochSummary.validatorBalance;

      EpochNumber epochs_since_finality =
          spec.get_current_epoch(state).increment().minus(state.getFinalizedEpoch());

      if (epochs_since_finality.lessEqual(EpochNumber.of(4))) {
        summary.noFinality = false;

        // Some helper variables
        List<PendingAttestation> previous_epoch_attestations =
            state.getPreviousEpochAttestations().listCopy();
        List<PendingAttestation> boundary_attestations = spec.get_previous_epoch_boundary_attestations(state);
        Gwei boundary_attesting_balance = spec.get_attesting_balance(state, boundary_attestations);
        Gwei total_balance = spec.get_previous_total_balance(state);
        Gwei total_attesting_balance = spec.get_attesting_balance(state, previous_epoch_attestations);
        List<PendingAttestation> matching_head_attestations =
            spec.get_previous_epoch_matching_head_attestations(state);
        Gwei matching_head_balance = spec.get_attesting_balance(state, matching_head_attestations);

        // Process rewards or penalties for all validators
        List<ValidatorIndex> active_validator_indices =
            spec.get_active_validator_indices(state.getValidatorRegistry(), spec.get_previous_epoch(state));

        for (ValidatorIndex index : active_validator_indices) {
          // Expected FFG source
          if (spec.get_attesting_indices(state, previous_epoch_attestations).contains(index)) {
            summary.attestationRewards.put(index,
                spec.get_base_reward(state, index).mulDiv(total_attesting_balance, total_balance));
            // Inclusion speed bonus
            summary.inclusionDistanceRewards.put(index,
                spec.get_base_reward(state, index)
                    .mulDiv(Gwei.castFrom(spec.getConstants().getMinAttestationInclusionDelay()),
                        Gwei.castFrom(spec.inclusion_distance(state, index))));
          } else {
            summary.attestationPenalties.put(index, spec.get_base_reward(state, index));
          }

          // Expected FFG target
          if (spec.get_attesting_indices(state, boundary_attestations).contains(index)) {
            summary.boundaryAttestationRewards.put(index,
                spec.get_base_reward(state, index).mulDiv(boundary_attesting_balance, total_balance));
          } else {
            summary.boundaryAttestationPenalties.put(index, spec.get_base_reward(state, index));
          }

          // Expected head
          if (spec.get_attesting_indices(state, matching_head_attestations).contains(index)) {
            summary.beaconHeadAttestationRewards.put(index,
                spec.get_base_reward(state, index).mulDiv(matching_head_balance, total_balance));
          } else {
            summary.beaconHeadAttestationPenalties.put(index,
                spec.get_base_reward(state, index));
          }
        }

      } else {
        summary.noFinality = true;

        List<PendingAttestation> previous_epoch_attestations =
            state.getPreviousEpochAttestations().listCopy();
        List<PendingAttestation> boundary_attestations =
            spec.get_previous_epoch_boundary_attestations(state);
        List<PendingAttestation> matching_head_attestations =
            spec.get_previous_epoch_matching_head_attestations(state);
        List<ValidatorIndex> active_validator_indices =
            spec.get_active_validator_indices(state.getValidatorRegistry(), spec.get_previous_epoch(state));

        // for index in active_validator_indices:
        for (ValidatorIndex index : active_validator_indices) {
          if (!spec.get_attesting_indices(state, previous_epoch_attestations).contains(index)) {
            summary.attestationPenalties.put(index,
                spec.get_inactivity_penalty(state, index, epochs_since_finality));
          } else {
            // If a validator did attest, apply a small penalty for getting attestations included late
            summary.noFinalityPenalties.put(index, spec.get_base_reward(state, index).mulDiv(
                Gwei.castFrom(spec.getConstants().getMinAttestationInclusionDelay()),
                Gwei.castFrom(spec.inclusion_distance(state, index))));
          }

          if (!spec.get_attesting_indices(state, boundary_attestations).contains(index)) {
            summary.boundaryAttestationPenalties.put(index,
                spec.get_inactivity_penalty(state, index, epochs_since_finality));
          }
          if (!spec.get_attesting_indices(state, matching_head_attestations).contains(index)) {
            summary.beaconHeadAttestationPenalties.put(index, spec.get_base_reward(state, index));
          }
        }

        // Penalize slashed-but-inactive validators as though they were active but offline
        for (ValidatorIndex index : state.getValidatorRegistry().size()) {
          boolean eligible = !active_validator_indices.contains(index) &&
              state.getValidatorRegistry().get(index).getSlashed() &&
              spec.get_current_epoch(state).less(state.getValidatorRegistry().get(index).getWithdrawableEpoch());

          if (eligible) {
            summary.initiatedExitPenalties.put(index,
                spec.get_inactivity_penalty(state, index, epochs_since_finality).times(2)
                    .plus(spec.get_base_reward(state, index)));
          }
        }
      }


      SlotNumber previous_epoch_start_slot = spec.get_epoch_start_slot(spec.get_previous_epoch(state));
      SlotNumber current_epoch_start_slot = spec.get_epoch_start_slot(spec.get_current_epoch(state));
      for (SlotNumber slot : previous_epoch_start_slot.iterateTo(current_epoch_start_slot)) {
        List<ShardCommittee> committees_and_shards = spec.get_crosslink_committees_at_slot(state, slot);
        for (ShardCommittee committee_and_shard : committees_and_shards) {
          List<ValidatorIndex> crosslink_committee = committee_and_shard.getCommittee();
          ShardNumber shard = committee_and_shard.getShard();
          Pair<Hash32, List<ValidatorIndex>> winning_root_and_participants =
              spec.get_winning_root_and_participants(state, slot, shard);
          Gwei participating_balance = spec.get_total_balance(state, winning_root_and_participants.getValue1());
          Gwei total_balance = spec.get_total_balance(state, crosslink_committee);

          for (ValidatorIndex index : crosslink_committee) {
            if (winning_root_and_participants.getValue1().contains(index)) {
              summary.attestationInclusionRewards.put(index,
                  spec.get_base_reward(state, index).mulDiv(participating_balance, total_balance));
            }
          }
        }
      }


    }

    spec.update_justification_and_finalization(state);
    spec.process_crosslinks(state);
    spec.maybe_reset_eth1_period(state);
    spec.apply_rewards(state);
    List<ValidatorIndex> ejectedValidators = spec.process_ejections(state);
    spec.update_registry_and_shuffling_data(state);
    spec.process_slashings(state);
    spec.process_exit_queue(state);
    spec.finish_epoch_update(state);

    BeaconStateEx ret = new BeaconStateExImpl(state.createImmutable(),
        origState.getHeadBlockHash(), TransitionType.EPOCH);

    if (summary != null) {
      summary.ejectedValidators = ejectedValidators;
      summary.postState = ret;
    }

    logger.debug(() -> "Epoch transition result state: (" +
        spec.hash_tree_root(ret).toStringShort() + ") " + ret.toString(spec.getConstants()));

    return ret;
  }
}
