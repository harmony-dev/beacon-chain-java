package org.ethereum.beacon.consensus.transition;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.types.SlotNumber;

/**
 * Fills transition gap between {@code fromState.slot} and {@code targetSlot} with sequential
 * transition of empty slots.
 *
 * <p>Also, applies per-epoch transition if epoch switch occurs during the gap.
 *
 * <p>Produces a state that would be a result of per-slot transition with {@code block.slot} number.
 *
 * @see NextSlotTransition
 * @see EpochTransition
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.1/specs/core/0_beacon-chain.md#per-slot-processing">Per-slot
 *     processing</a> in the spec.
 */
public class EmptySlotTransition {

  private final SpecHelpers specHelpers;
  private final StateTransition<BeaconStateEx> perSlotTransition;
  private final StateTransition<BeaconStateEx> perEpochTransition;

  public EmptySlotTransition(
      SpecHelpers specHelpers,
      StateTransition<BeaconStateEx> perSlotTransition,
      StateTransition<BeaconStateEx> perEpochTransition) {
    this.specHelpers = specHelpers;
    this.perSlotTransition = perSlotTransition;
    this.perEpochTransition = perEpochTransition;
  }

  /**
   * Runs sequential state transitions.
   *
   * @param fromState a state to start sequential transitions from.
   * @param targetSlot a slot which per-slot transition is applied at last.
   * @return result of sequential transitions.
   */
  public BeaconStateEx apply(BeaconStateEx fromState, SlotNumber targetSlot) {
    BeaconStateEx result = fromState;
    while (result.getCanonicalState().getSlot().less(targetSlot)) {
      result = perSlotTransition.apply(null, result);
      if (specHelpers.is_epoch_end(result.getCanonicalState().getSlot())) {
        result = perEpochTransition.apply(null, result);
      }
    }

    return perSlotTransition.apply(null, result);
  }
}
