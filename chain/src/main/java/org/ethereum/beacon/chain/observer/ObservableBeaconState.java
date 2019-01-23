package org.ethereum.beacon.chain.observer;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

/** An observable chain state. */
public class ObservableBeaconState {
  private final BeaconBlock head;
  private final BeaconState latestSlotState;
  private final PendingOperations pendingOperations;

  public ObservableBeaconState(
      BeaconBlock head, BeaconState latestSlotState, PendingOperations pendingOperations) {
    this.head = head;
    this.latestSlotState = latestSlotState;
    this.pendingOperations = pendingOperations;
  }

  public BeaconBlock getHead() {
    return head;
  }

  public BeaconState getLatestSlotState() {
    return latestSlotState;
  }

  public PendingOperations getPendingOperations() {
    return pendingOperations;
  }
}
