package org.ethereum.beacon.consensus;

import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.consensus.transition.ExtendedSlotTransition;
import org.ethereum.beacon.consensus.transition.PerBlockTransition;
import org.ethereum.beacon.consensus.transition.PerEpochTransition;
import org.ethereum.beacon.consensus.transition.PerSlotTransition;

/** Instantiates high level state transitions. */
public abstract class StateTransitions {
  public StateTransitions() {}

  public static EmptySlotTransition preBlockTransition(BeaconChainSpec spec) {
    ExtendedSlotTransition extendedSlotTransition = ExtendedSlotTransition.create(spec);
    return new EmptySlotTransition(extendedSlotTransition);
  }

  public static PerBlockTransition blockTransition(BeaconChainSpec spec) {
    return new PerBlockTransition(spec);
  }
}
