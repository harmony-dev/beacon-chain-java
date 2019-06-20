package org.ethereum.beacon.consensus.transition;

import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.spec.SpecStateTransition;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.types.SlotNumber;

/**
 * Runs state transition up to a given slot as if all slots were empty, i.e. without a block.
 *
 * <p>Reflects {@link SpecStateTransition#process_slots(MutableBeaconState, SlotNumber)}
 * function behaviour.
 *
 * @see ExtendedSlotTransition
 */
public class EmptySlotTransition {

  private final ExtendedSlotTransition onSlotTransition;

  public EmptySlotTransition(ExtendedSlotTransition onSlotTransition) {
    this.onSlotTransition = onSlotTransition;
  }

  /**
   * Applies {@link #onSlotTransition} to a source state until given {@code slot} number is reached.
   *
   * @param source source state.
   * @param tillSlot slot number, inclusively.
   * @return modified source state.
   */
  public BeaconStateEx apply(BeaconStateEx source, SlotNumber tillSlot) {
    BeaconStateEx result = source;
    while (result.getSlot().less(tillSlot)) {
      result = onSlotTransition.apply(result);
    }
    return result;
  }
}
