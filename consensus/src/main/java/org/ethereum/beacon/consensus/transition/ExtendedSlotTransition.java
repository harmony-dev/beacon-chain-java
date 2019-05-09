package org.ethereum.beacon.consensus.transition;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
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
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.1/specs/core/0_beacon-chain.md#beacon-chain-state-transition-function">Beacon
 *     chain state transition function</a> in the spec.
 * @see PerBlockTransition
 */
public class ExtendedSlotTransition implements StateTransition<BeaconStateEx> {

  private final StateTransition<BeaconStateEx> stateCaching;
  private final PerEpochTransition perEpochTransition;
  private final StateTransition<BeaconStateEx> perSlotTransition;
  private final BeaconChainSpec spec;

  public ExtendedSlotTransition(
      StateTransition<BeaconStateEx> stateCaching,
      PerEpochTransition perEpochTransition,
      StateTransition<BeaconStateEx> perSlotTransition,
      BeaconChainSpec spec) {
    this.stateCaching = stateCaching;
    this.perEpochTransition = perEpochTransition;
    this.perSlotTransition = perSlotTransition;
    this.spec = spec;
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx source) {
    return apply(source, null);
  }

  public EpochTransitionSummary applyWithSummary(BeaconStateEx stateEx) {
    EpochTransitionSummary summary = new EpochTransitionSummary();
    apply(stateEx, summary);
    return summary;
  }

  private BeaconStateEx apply(BeaconStateEx source, EpochTransitionSummary summary) {
    BeaconStateEx cachedState = stateCaching.apply(source);
    BeaconStateEx newEpochState = cachedState;
    // The steps below happen when (state.slot + 1) % SLOTS_PER_EPOCH == 0.
    if (cachedState.getSlot().increment().modulo(spec.getConstants().getSlotsPerEpoch())
        .equals(SlotNumber.ZERO)) {
      newEpochState = perEpochTransition.apply(cachedState, summary);
    }
    BeaconStateEx newSlotState = perSlotTransition.apply(newEpochState);

    return newSlotState;
  }
}
