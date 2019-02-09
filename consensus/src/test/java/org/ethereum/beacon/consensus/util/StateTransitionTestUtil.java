package org.ethereum.beacon.consensus.util;

import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.transition.BeaconStateEx;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.state.Eth1Data;
import tech.pegasys.artemis.ethereum.core.Hash32;

public abstract class StateTransitionTestUtil {
  private StateTransitionTestUtil() {}

  public static BlockTransition<BeaconStateEx> createPerBlockTransition() {
    return (source, block) -> {
      MutableBeaconState newState = source.getCanonicalState().createMutableCopy();
      newState.setSlot(block.getSlot());
      return new BeaconStateEx(newState, source.getLatestChainBlockHash());
    };
  }

  public static StateTransition<BeaconStateEx> createStateWithNoTransition() {
    return (source) -> source;
  }
}
