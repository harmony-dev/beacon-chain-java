package org.ethereum.beacon.wire.sync;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.wire.Feedback;
import org.reactivestreams.Publisher;

public interface SyncManager {
  Publisher<Feedback<BeaconBlock>> getBlocksReadyToImport();

  void start();

  void stop();

  Publisher<SyncMode> getSyncModeStream();

  Publisher<SyncStatus> getSyncStatusStream();

  enum SyncMode {
    Long,
    Short
  }

  class SyncStatus {
    private final boolean syncing;
    private final SlotNumber start;
    private final SlotNumber bestKnown;
    private final SlotNumber current;

    public SyncStatus(boolean syncing, SlotNumber start, SlotNumber bestKnown, SlotNumber current) {
      this.syncing = syncing;
      this.start = start;
      this.bestKnown = bestKnown;
      this.current = current;
    }

    public boolean isSyncing() {
      return syncing;
    }

    public SlotNumber getStart() {
      return start;
    }

    public SlotNumber getBestKnown() {
      return bestKnown;
    }

    public SlotNumber getCurrent() {
      return current;
    }
  }
}
