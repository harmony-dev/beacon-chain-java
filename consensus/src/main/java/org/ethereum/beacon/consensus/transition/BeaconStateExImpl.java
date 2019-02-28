package org.ethereum.beacon.consensus.transition;

import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Class to hold additional state info which is not included to the
 * canonical spec BeaconState but is wanted for state transitions
 */
@SSZSerializable(instanceGetter = "getDelegate")
public class BeaconStateExImpl extends DelegateBeaconState implements BeaconStateEx {

  private final Hash32 latestChainBlock;
  private final TransitionType lastTransition;

  public BeaconStateExImpl(BeaconState canonicalState,
      Hash32 latestChainBlock,
      TransitionType lastTransition) {
    super(canonicalState);
    this.latestChainBlock = latestChainBlock;
    this.lastTransition = lastTransition;
  }

  /**
   * @param canonicalState regular BeaconState
   * @param latestChainBlock  latest block which was processed on this state chain
   */
  public BeaconStateExImpl(BeaconState canonicalState, Hash32 latestChainBlock) {
    this(canonicalState, latestChainBlock, TransitionType.UNKNOWN);
  }

  @Override
  public Hash32 getHeadBlockHash() {
    return latestChainBlock;
  }

  @Override
  public TransitionType getTransition() {
    return lastTransition;
  }

  @Override
  public String toString() {
    return toString(null);
  }
}
