package org.ethereum.beacon.consensus.util;

import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.core.MutableBeaconState;

public abstract class StateTransitionTestUtil {
  private StateTransitionTestUtil() {}

  public static BlockTransition<BeaconStateEx> createPerBlockTransition() {
    return (source, block) -> {
      MutableBeaconState newState = source.createMutableCopy();
      newState.setSlot(block.getSlot());
      return new BeaconStateExImpl(newState, source.getHeadBlockHash());
    };
  }

  public static StateTransition<BeaconStateEx> createStateWithNoTransition() {
    return (source) -> source;
  }

  public static StateTransition<BeaconStateEx> createNextSlotTransition() {
    return (source) -> {
      MutableBeaconState result = source.createMutableCopy();
      result.setSlot(result.getSlot().increment());
      return new BeaconStateExImpl(result, source.getHeadBlockHash());
    };
  }
}
