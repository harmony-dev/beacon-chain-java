package org.ethereum.beacon.wire.sync;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSync;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;

// TODO: revisit and complete this interface
public interface SyncManager {

  Disposable subscribeToOnlineBlocks(Publisher<Feedback<BeaconBlock>> onlineBlocks);

  Disposable subscribeToFinalizedBlocks(Publisher<BeaconBlock> finalBlocks);

  void setSyncApi(WireApiSync syncApi);

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
    private final SyncMode syncMode;

    public SyncStatus(boolean syncing, SlotNumber start, SlotNumber bestKnown, SlotNumber current, SyncMode syncMode) {
      this.syncing = syncing;
      this.start = start;
      this.bestKnown = bestKnown;
      this.current = current;
      this.syncMode = syncMode;
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

    public SyncMode getSyncMode() {
      return syncMode;
    }
  }
}
