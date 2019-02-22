package org.ethereum.beacon.consensus.transition;

import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Class to hold additional state info which is not included to the
 * canonical spec BeaconState but is wanted for state transitions
 */
public class BeaconStateEx {

  public enum TransitionType {
    INITIAL,
    SLOT,
    BLOCK,
    EPOCH,
    UNKNOWN;

    public boolean canBeAppliedAfter(TransitionType beforeTransition) {
      switch (beforeTransition) {
        case UNKNOWN: return true;
        case INITIAL:
        case EPOCH:
          switch (this) {
            case INITIAL: return false;
            case SLOT: return true;
            case BLOCK: return false;
            case EPOCH: return false;
          }
        case SLOT:
          switch (this) {
            case INITIAL: return false;
            case SLOT: return true;
            case BLOCK: return true;
            case EPOCH: return true;
          }
        case BLOCK:
          switch (this) {
            case INITIAL: return false;
            case SLOT: return true;
            case BLOCK: return false;
            case EPOCH: return true;
          }
      }
      throw new RuntimeException("Impossible");
    }

    public void checkCanBeAppliedAfter(TransitionType beforeTransition) throws RuntimeException {
      if (!canBeAppliedAfter(beforeTransition)) {
        throw new RuntimeException(
            this + " transition can't be applied after " + beforeTransition + " transition");
      }
    }
  }

  private final BeaconState canonicalState;
  private final Hash32 latestChainBlock;
  private final TransitionType lastTransition;

  public BeaconStateEx(BeaconState canonicalState,
      Hash32 latestChainBlock,
      TransitionType lastTransition) {
    this.canonicalState = canonicalState;
    this.latestChainBlock = latestChainBlock;
    this.lastTransition = lastTransition;
  }

  /**
   * @param canonicalState regular BeaconState
   * @param latestChainBlock  latest block which was processed on this state chain
   */
  public BeaconStateEx(BeaconState canonicalState, Hash32 latestChainBlock) {
    this(canonicalState, latestChainBlock, TransitionType.UNKNOWN);
  }

  public BeaconState getCanonicalState() {
    return canonicalState;
  }

  public Hash32 getLatestChainBlockHash() {
    return latestChainBlock;
  }

  public TransitionType getLastTransition() {
    return lastTransition;
  }

  @Override
  public String toString() {
    return toString(null);
  }

  public String toString(
      @Nullable ChainSpec spec) {
    return "BeaconStateEx[latestBlock=" + latestChainBlock.toStringShort()
        + (lastTransition == TransitionType.UNKNOWN ? "" : ", lastTransition=" + lastTransition)
        + ", " + canonicalState.toStringShort(spec);
  }
}
