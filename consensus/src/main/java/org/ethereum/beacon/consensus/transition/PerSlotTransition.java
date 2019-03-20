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
 * Per-epoch transition function.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/core/0_beacon-chain.md#per-slot-processing">Per-slot
 *     processing</a> in the spec.
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

    // state.slot += 1
    SlotNumber newSlot = state.getSlot().increment();
    state.setSlot(newSlot);

    //  Set state.latest_block_roots[(state.slot - 1) % SLOTS_PER_HISTORICAL_ROOT] = previous_block_root.
    state.getLatestBlockRoots().set(
        state.getSlot().decrement().modulo(spec.getConstants().getSlotsPerHistoricalRoot()),
        stateEx.getHeadBlockHash());

    // If state.slot % SLOTS_PER_HISTORICAL_ROOT == 0
    // append merkle_root(state.latest_block_roots) to state.batched_block_roots
    if (state.getSlot().modulo(spec.getConstants().getSlotsPerHistoricalRoot()).getIntValue() == 0) {
      state.getBatchedBlockRoots().add(spec.merkle_root(state.getLatestBlockRoots()));
    }

    BeaconStateEx ret =
        new BeaconStateExImpl(
            state.createImmutable(), stateEx.getHeadBlockHash(), TransitionType.SLOT);

    logger.trace(() -> "Slot transition result state: (" +
        spec.hash_tree_root(ret).toStringShort() + ") " + ret.toString(spec.getConstants()));

    return ret;
  }
}
