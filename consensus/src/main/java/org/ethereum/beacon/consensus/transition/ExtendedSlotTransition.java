package org.ethereum.beacon.consensus.transition;

import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.types.SlotNumber;

/**
 * An extended state transition that happens at the beginning of each slot.
 *
 * <p>Runs following steps of Beacon chain state transition function:
 *
 * <ol>
 *   <li>State caching, which happens at the start of every slot.
 *   <li>The per-epoch transitions, which happens at the start of the first slot of every epoch.
 *   <li>The per-slot transitions, which happens at every slot.
 * </ol>
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/core/0_beacon-chain.md#beacon-chain-state-transition-function">Beacon
 *     chain state transition function</a> in the spec.
 * @see PerBlockTransition
 */
public class ExtendedSlotTransition implements StateTransition<BeaconStateEx> {

  private final StateTransition<BeaconStateEx> stateCaching;
  private final StateTransition<BeaconStateEx> perEpochTransition;
  private final StateTransition<BeaconStateEx> perSlotTransition;
  private final SpecHelpers spec;

  public ExtendedSlotTransition(
      StateTransition<BeaconStateEx> stateCaching,
      StateTransition<BeaconStateEx> perEpochTransition,
      StateTransition<BeaconStateEx> perSlotTransition,
      SpecHelpers spec) {
    this.stateCaching = stateCaching;
    this.perEpochTransition = perEpochTransition;
    this.perSlotTransition = perSlotTransition;
    this.spec = spec;
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx source) {
    BeaconStateEx result = source;
    result = stateCaching.apply(source);
    // The steps below happen when (state.slot + 1) % SLOTS_PER_EPOCH == 0.
    if (result.getSlot().increment().modulo(spec.getConstants().getSlotsPerEpoch())
        .equals(SlotNumber.ZERO)) {
      result = perEpochTransition.apply(result);
    }
    result = perSlotTransition.apply(result);

    return result;
  }
}
