package org.ethereum.beacon.consensus.transition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.types.SlotNumber;

/**
 * Per-slot transition function.
 *
 * <p>Includes all steps from <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.0/specs/core/0_beacon-chain.md#beacon-chain-state-transition-function">Beacon
 * chain state transition function</a> except per-block processing.
 *
 * @see PerBlockTransition
 */
public class PerSlotTransition implements StateTransition<BeaconStateEx> {
  private static final Logger logger = LogManager.getLogger(PerSlotTransition.class);

  private final SpecHelpers spec;

  public PerSlotTransition(SpecHelpers spec) {
    this.spec = spec;
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx stateEx) {
    logger.trace(() -> "Applying slot transition to state: (" +
        spec.hash_tree_root(stateEx).toStringShort() + ") " + stateEx.toString(spec.getConstants()));
    TransitionType.SLOT.checkCanBeAppliedAfter(stateEx.getTransition());

    MutableBeaconState state = stateEx.createMutableCopy();

    if (state.getSlot().greater(spec.getConstants().getGenesisSlot())) {

      // 1. State caching, which happens at the start of every slot.
      // At every slot > GENESIS_SLOT run the following function:
      spec.cache_state(state);

      // 2. The per-epoch transitions, which happens at the start of the first slot of every epoch.
      // The steps below happen when state.slot > GENESIS_SLOT and (state.slot + 1) % SLOTS_PER_EPOCH == 0.
      if (state.getSlot().increment().modulo(spec.getConstants().getSlotsPerEpoch())
          .equals(SlotNumber.ZERO)) {
        spec.update_justification_and_finalization(state);
        spec.process_crosslinks(state);
        spec.maybe_reset_eth1_period(state);
        spec.apply_rewards(state);
        spec.process_ejections(state);
        spec.update_registry_and_shuffling_data(state);
        spec.process_slashings(state);
        spec.process_exit_queue(state);
        spec.finish_epoch_update(state);
      }

      // 3. The per-slot transitions, which happens at every slot.
      // At every slot > GENESIS_SLOT run the following function:
      spec.advance_slot(state);
    }

    BeaconStateEx ret =
        new BeaconStateExImpl(
            state.createImmutable(), stateEx.getHeadBlockHash(), TransitionType.SLOT);

    logger.trace(() -> "Slot transition result state: (" +
        spec.hash_tree_root(ret).toStringShort() + ") " + ret.toString(spec.getConstants()));

    return ret;
  }
}
