package org.ethereum.beacon.consensus;

import javax.annotation.Nullable;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Class to hold additional state info which is not included to the
 * canonical spec BeaconState
 */
public interface BeaconStateEx extends BeaconState {

  static BeaconStateEx getEmpty() {
    return new BeaconStateExImpl(BeaconState.getEmpty(), Hash32.ZERO, TransitionType.UNKNOWN);
  }

  Hash32 getHeadBlockHash();

  TransitionType getTransition();

  default String toString(
      @Nullable ChainSpec spec) {
    return "BeaconStateEx[headBlock=" + getHeadBlockHash().toStringShort()
        + (getTransition() == TransitionType.UNKNOWN ? "" : ", lastTransition=" + getTransition())
        + ", " + toStringShort(spec);
  }
}
