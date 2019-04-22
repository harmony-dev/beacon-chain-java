package org.ethereum.beacon.consensus;

import java.util.function.Function;
import javax.annotation.Nullable;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Class to hold additional state info which is not included to the
 * canonical spec BeaconState
 */
public interface BeaconStateEx extends BeaconState {

  static BeaconStateEx getEmpty() {
    return new BeaconStateExImpl(BeaconState.getEmpty(), TransitionType.UNKNOWN);
  }

  static BeaconStateEx getEmpty(SpecConstants specConst) {
    return new BeaconStateExImpl(BeaconState.getEmpty(specConst), TransitionType.UNKNOWN);
  }


  TransitionType getTransition();

  default String toString(
      @Nullable SpecConstants constants,
      @Nullable Function<Object, Hash32> blockHasher) {
    return "BeaconStateEx[headBlock="
        + (blockHasher != null ? blockHasher.apply(getLatestBlockHeader()) : Hash32.ZERO)
            .toStringShort()
        + (getTransition() == TransitionType.UNKNOWN ? "" : ", lastTransition=" + getTransition())
        + ", "
        + toStringShort(constants);
  }
}
