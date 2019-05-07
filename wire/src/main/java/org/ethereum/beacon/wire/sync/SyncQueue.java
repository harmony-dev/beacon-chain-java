package org.ethereum.beacon.wire.sync;

import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.wire.Feedback;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public interface SyncQueue {

  Publisher<BlockRequest> getBlockRequestsStream();

  Publisher<Feedback<BeaconBlock>> getBlocksStream();

  Disposable subscribeToFinalBlocks(Flux<BeaconBlock> finalBlockRootStream);

  Disposable subscribeToNewBlocks(Publisher<Feedback<List<BeaconBlock>>> blocksStream);

  class BlockRequest {
    private final SlotNumber startSlot;
    private final Hash32 startRoot;
    private final UInt64 maxCount;
    private final boolean reverse;
    private final UInt64 step;

    public BlockRequest(SlotNumber startSlot, Hash32 startRoot,
        int maxCount, boolean reverse, int step) {
      this.startSlot = startSlot;
      this.startRoot = startRoot;
      this.maxCount = UInt64.valueOf(maxCount);
      this.reverse = reverse;
      this.step = UInt64.valueOf(step);
    }

    public Optional<SlotNumber> getStartSlot() {
      return Optional.ofNullable(startSlot);
    }

    public Optional<Hash32> getStartRoot() {
      return Optional.ofNullable(startRoot);
    }

    public UInt64 getMaxCount() {
      return maxCount;
    }

    public boolean isReverse() {
      return reverse;
    }

    public UInt64 getStep() {
      return step;
    }
  }
}
