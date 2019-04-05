package org.ethereum.beacon.consensus;

import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.consensus.transition.ExtendedSlotTransition;
import org.ethereum.beacon.consensus.transition.PerBlockTransition;
import org.ethereum.beacon.consensus.transition.PerEpochTransition;
import org.ethereum.beacon.consensus.transition.PerSlotTransition;
import org.ethereum.beacon.consensus.transition.StateCachingTransition;

/** Instantiates high level state transitions. */
public abstract class StateTransitions {
  public StateTransitions() {}

  public static EmptySlotTransition preBlockTransition(BeaconChainSpec spec) {
    PerSlotTransition perSlotTransition = new PerSlotTransition(spec);
    PerEpochTransition perEpochTransition = new PerEpochTransition(spec);
    StateCachingTransition stateCachingTransition = new StateCachingTransition(spec);
    ExtendedSlotTransition extendedSlotTransition =
        new ExtendedSlotTransition(
            stateCachingTransition, perEpochTransition, perSlotTransition, spec);
    return new EmptySlotTransition(extendedSlotTransition);
  }

  public static PerBlockTransition blockTransition(BeaconChainSpec spec) {
    return new PerBlockTransition(spec);
  }
}
