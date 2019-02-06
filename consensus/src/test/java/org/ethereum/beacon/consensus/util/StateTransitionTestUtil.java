package org.ethereum.beacon.consensus.util;

import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconState;

public abstract class StateTransitionTestUtil {
  private StateTransitionTestUtil() {}

  public static StateTransition<BeaconState> createSlotIncrementingTransition() {
    return (block, state) -> state.createMutableCopy().withSlot(block.getSlot());
  }
}
