package org.ethereum.beacon.chain.observer;

import com.google.common.base.Objects;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ObservableBeaconState that = (ObservableBeaconState) o;
    return Objects.equal(head, that.head) &&
        Objects.equal(latestSlotState, that.latestSlotState) &&
        Objects.equal(pendingOperations, that.pendingOperations);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(head, latestSlotState, pendingOperations);
  }
}
