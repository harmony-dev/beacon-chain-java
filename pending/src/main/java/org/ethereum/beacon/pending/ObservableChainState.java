package org.ethereum.beacon.pending;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

/**
 * An observable chain state includes:
 *
 * <ul>
 *   <li>{@link BeaconBlock} that is a head of the chain;
 *   <li>{@link BeaconState} after recent slot has been processed;
 *   <li>{@link PendingState} constantly updating with latest messages from the net.
 * </ul>
 */
public class ObservableChainState {
  private final BeaconBlock head;
  private final BeaconState recentSlotState;
  private final PendingState pendingState;

  public ObservableChainState(
      BeaconBlock head, BeaconState recentSlotState, PendingState pendingState) {
    this.head = head;
    this.recentSlotState = recentSlotState;
    this.pendingState = pendingState;
  }

  public BeaconBlock getHead() {
    return head;
  }

  public BeaconState getRecentSlotState() {
    return recentSlotState;
  }

  public PendingState getPendingState() {
    return pendingState;
  }
}
