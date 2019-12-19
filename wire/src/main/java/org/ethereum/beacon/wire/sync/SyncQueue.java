package org.ethereum.beacon.wire.sync;

import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.wire.Feedback;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * The class which declares what blocks are wanted to be downloaded, consumes
 * downloaded blocks, builds chains of blocks linked to the finalized block and
 * streams them for importing.
 */
public interface SyncQueue {

  /**
   * Potentially unbounded stream of blocks wanted to be downloaded.
   * The stream may be unbounded because the queue tries to retrieve any
   * new blocks above the final block again and again with the hope that
   * something new can be discovered. Thus the consumer should have a mechanism
   * of limiting these requests to prevent traffic overhead.
   */
  Publisher<BlockRequest> getBlockRequestsStream();

  /**
   * The stream of blocks ready to be imported to the blockchain.
   * Any issued block must be a child of some block issued before.
   * Blocks are wrapped to a {@link Feedback} instance so
   * block verification and importing result should be reported via this {@link Feedback}
   */
  Publisher<Feedback<SignedBeaconBlock>> getBlocksStream();

  /**
   *  finalBlockRootStream notifies the {@link SyncQueue} on finalized blocks
   *  so the queue may stick to those blocks and perform necessary cleanup
   *  of outdated blocks
   */
  Disposable subscribeToFinalBlocks(Flux<SignedBeaconBlock> finalBlockRootStream);

  /**
   * All new blocks are streamed via blocksStream.
   * Those blocks may include:
   * - downloaded per {@link SyncQueue} requests blocks
   * - new fresh blocks broadcasted from remote parties
   * - new blocks proposed by local validators
   */
  Disposable subscribeToNewBlocks(Publisher<Feedback<List<SignedBeaconBlock>>> blocksStream);

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
