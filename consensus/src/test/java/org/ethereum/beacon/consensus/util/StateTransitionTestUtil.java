package org.ethereum.beacon.consensus.util;

import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;

public abstract class StateTransitionTestUtil {
  private StateTransitionTestUtil() {}

  public static BlockTransition<BeaconState> createSlotFromBlockTransition() {
    return (state, block) -> {
      MutableBeaconState newState = state.createMutableCopy();
      newState.setSlot(block.getSlot());
      return newState;
    };
  }
}
