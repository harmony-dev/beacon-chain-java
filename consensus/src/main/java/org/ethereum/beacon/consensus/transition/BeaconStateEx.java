package org.ethereum.beacon.consensus.transition;

import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.Time;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Class to hold additional state info which is not included to the
 * canonical spec BeaconState but is wanted for state transitions
 */
public class BeaconStateEx {
  private final BeaconState canonicalState;
  private final Hash32 latestChainBlock;

  /**
   * @param canonicalState regular BeaconState
   * @param latestChainBlock  latest block which was processed on this state chain
   */
  public BeaconStateEx(BeaconState canonicalState, Hash32 latestChainBlock) {
    this.canonicalState = canonicalState;
    this.latestChainBlock = latestChainBlock;
  }

  public BeaconState getCanonicalState() {
    return canonicalState;
  }

  public Hash32 getLatestChainBlockHash() {
    return latestChainBlock;
  }

  @Override
  public String toString() {
    return toString(null);
  }

  public String toString(
      @Nullable ChainSpec spec) {
    return "BeaconStateEx[latestBlock=" + latestChainBlock.toStringShort()
        + ", " + canonicalState.toStringShort(spec);
  }
}
