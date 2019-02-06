package org.ethereum.beacon.consensus.util;

import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;

public abstract class StateTransitionTestUtil {
  private StateTransitionTestUtil() {}

  public static StateTransition<BeaconState> createSlotFromBlockTransition() {
    return (block, state) -> {
      MutableBeaconState newState = state.createMutableCopy();
      newState.setSlot(block.getSlot());
      return newState;
    };
  }
}
