package org.ethereum.beacon.validator.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.wire.sync.SyncManager;

import java.math.BigInteger;

public class SyncingResponse {
  @JsonProperty("is_syncing")
  private boolean syncing;
  @JsonProperty("sync_status")
  private SyncStatus syncStatus;

  public SyncingResponse() {
  }

  public SyncingResponse(boolean syncing, BigInteger startingSlot, BigInteger currentSlot, BigInteger highestSlot) {
    this.syncing = syncing;
    this.syncStatus = new SyncStatus(startingSlot, currentSlot, highestSlot);
  }

  public static SyncingResponse create(SyncManager.SyncStatus status) {
    return new SyncingResponse(
        status.isSyncing(),
        status.getStart() == null
            ? BigInteger.ZERO
            : status.getStart().toBI(),
        status.getCurrent() == null
            ? BigInteger.ZERO
            : status.getCurrent().toBI(),
        status.getBestKnown() == null
            ? BigInteger.ZERO
            : status.getBestKnown().toBI());
  }

  public boolean isSyncing() {
    return syncing;
  }

  public void setSyncing(boolean syncing) {
    this.syncing = syncing;
  }

  public SyncStatus getSyncStatus() {
    return syncStatus;
  }

  public void setSyncStatus(SyncStatus syncStatus) {
    this.syncStatus = syncStatus;
  }

  public static class SyncStatus {
    @JsonProperty("starting_slot")
    private BigInteger startingSlot;
    @JsonProperty("current_slot")
    private BigInteger currentSlot;
    @JsonProperty("highest_slot")
    private BigInteger highestSlot;

    public SyncStatus() {
    }

    public SyncStatus(BigInteger startingSlot, BigInteger currentSlot, BigInteger highestSlot) {
      this.startingSlot = startingSlot;
      this.currentSlot = currentSlot;
      this.highestSlot = highestSlot;
    }

    public BigInteger getStartingSlot() {
      return startingSlot;
    }

    public void setStartingSlot(BigInteger startingSlot) {
      this.startingSlot = startingSlot;
    }

    public BigInteger getCurrentSlot() {
      return currentSlot;
    }

    public void setCurrentSlot(BigInteger currentSlot) {
      this.currentSlot = currentSlot;
    }

    public BigInteger getHighestSlot() {
      return highestSlot;
    }

    public void setHighestSlot(BigInteger highestSlot) {
      this.highestSlot = highestSlot;
    }
  }
}
