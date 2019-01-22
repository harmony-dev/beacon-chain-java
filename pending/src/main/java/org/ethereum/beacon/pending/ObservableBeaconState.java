package org.ethereum.beacon.pending;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import tech.pegasys.artemis.ethereum.core.Hash32;

/** An observable chain state. */
public class ObservableBeaconState {
  private final BeaconBlock head;
  private final BeaconState latestSlotState;
  private final Hash32 latestDepositRoot;
  private final PendingOperations pendingOperations;

  public ObservableBeaconState(
      BeaconBlock head,
      BeaconState latestSlotState,
      PendingOperations pendingOperations,
      Hash32 latestDepositRoot) {
    this.head = head;
    this.latestSlotState = latestSlotState;
    this.pendingOperations = pendingOperations;
    this.latestDepositRoot = latestDepositRoot;
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

  public Hash32 getLatestDepositRoot() {
    return latestDepositRoot;
  }
}
