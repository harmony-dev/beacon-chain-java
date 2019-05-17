package org.ethereum.beacon.consensus.spec;

import org.ethereum.beacon.core.MutableBeaconState;

/**
 * Per slot processing.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.1/specs/core/0_beacon-chain.md#per-slot-processing">Per-slot
 *     processing</a> in the spec.
 */
public interface SlotProcessing extends HelperFunction {

  /*
    At every slot > GENESIS_SLOT run the following function:
    Note: this function mutates beacon state
   */
  default void advance_slot(MutableBeaconState state) {
    state.setSlot(state.getSlot().increment());
  }
}
