package org.ethereum.beacon.consensus.transition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
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

  private final SpecHelpers specHelpers;
  private final ChainSpec spec;

  public PerSlotTransition(SpecHelpers specHelpers) {
    this.specHelpers = specHelpers;
    this.spec = specHelpers.getChainSpec();
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx stateEx) {
    logger.debug(() -> "Applying slot transition to state: (" +
        specHelpers.hash_tree_root(stateEx.getCanonicalState()).toStringShort() + ") " + stateEx.toString(spec));

    MutableBeaconState state = stateEx.getCanonicalState().createMutableCopy();

    // state.slot += 1
    SlotNumber newSlot = state.getSlot().increment();
    state.setSlot(newSlot);

    //  Set state.latest_block_roots[(state.slot - 1) % LATEST_BLOCK_ROOTS_LENGTH] = previous_block_root.
    state.getLatestBlockRoots().set(
        state.getSlot().decrement().modulo(spec.getLatestBlockRootsLength()),
        stateEx.getLatestChainBlockHash());

    // If state.slot % LATEST_BLOCK_ROOTS_LENGTH == 0
    // append merkle_root(state.latest_block_roots) to state.batched_block_roots
    if (state.getSlot().modulo(spec.getLatestBlockRootsLength()).getIntValue() == 0) {
      state.getBatchedBlockRoots().add(specHelpers.merkle_root(state.getLatestBlockRoots()));
    }

    BeaconStateEx ret = new BeaconStateEx(state.createImmutable(), stateEx.getLatestChainBlockHash());

    logger.debug(() -> "Slot transition result state: (" +
        specHelpers.hash_tree_root(ret.getCanonicalState()).toStringShort() + ") " + ret.toString(spec));

    return ret;
  }
}
