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
import org.ethereum.beacon.core.state.PendingAttestation;
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
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.0/specs/core/0_beacon-chain.md#per-epoch-processing">Per-epoch
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
    // 2. The per-epoch transitions, which happens at the start of the first slot of every epoch.
    // The steps below happen when state.slot > GENESIS_SLOT and (state.slot + 1) % SLOTS_PER_EPOCH == 0.
    if (!origState.getSlot().increment().modulo(spec.getConstants().getSlotsPerEpoch())
        .equals(SlotNumber.ZERO)) {
      return origState;
    }

    logger.debug(() -> "Applying epoch transition to state: (" +
        spec.hash_tree_root(origState).toStringShort() + ") " + origState.toString(spec.getConstants()));

    TransitionType.EPOCH.checkCanBeAppliedAfter(origState.getTransition());

    if (summary != null) {
      summary.preState = origState;
    }

    MutableBeaconState state = origState.createMutableCopy();

    spec.update_justification_and_finalization(state);
    spec.process_crosslinks(state);
    spec.maybe_reset_eth1_period(state);
    spec.apply_rewards(state);
    spec.process_ejections(state);
    spec.update_registry_and_shuffling_data(state);
    spec.process_slashings(state);
    spec.process_exit_queue(state);
    spec.finish_epoch_update(state);

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
