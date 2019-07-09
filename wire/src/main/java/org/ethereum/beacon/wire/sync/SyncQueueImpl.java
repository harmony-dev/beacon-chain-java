package org.ethereum.beacon.wire.sync;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.wire.Feedback;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;

public class SyncQueueImpl implements SyncQueue {
  private static final Logger logger = LogManager.getLogger(SyncQueueImpl.class);

  private final BeaconBlockTree blockTree;
  private final int maxBlocksRequest;
  private final int maxHeightFromFinal;

  private final ReplayProcessor<Feedback<BeaconBlock>> readyBlocks = ReplayProcessor.cacheLast();
  private final ReplayProcessor<Flux<BlockRequest>> blockRequests = ReplayProcessor.cacheLast();
  private BeaconBlock finalBlock;

  public SyncQueueImpl(BeaconBlockTree blockTree, int maxBlocksRequest, int maxHeightFromFinal) {
    this.blockTree = blockTree;
    this.maxBlocksRequest = maxBlocksRequest;
    this.maxHeightFromFinal = maxHeightFromFinal;
  }

  public SyncQueueImpl(BeaconBlockTree blockTree) {
    this(blockTree, 128, 4096);
  }

  @Override
  public Publisher<BlockRequest> getBlockRequestsStream() {
    return Flux.switchOnNext(blockRequests, 1);
  }

  @Override
  public Publisher<Feedback<BeaconBlock>> getBlocksStream() {
    return readyBlocks;
  }

  protected Flux<BlockRequest> createBlockRequests() {
    return Flux.generate(
        () -> finalBlock.getSlot(),
        (slot, sink) -> {
          if (slot.greater(finalBlock.getSlot().plus(maxHeightFromFinal))) {
            slot = finalBlock.getSlot();
          }
          sink.next(new BlockRequest(slot, null, maxBlocksRequest, false, 0));
          SlotNumber tmp =  slot.plus(maxBlocksRequest);
          System.out.println(String.format("Request: %s, %s, %s, %s, %s", tmp, slot, finalBlock, maxHeightFromFinal, maxBlocksRequest));
          return tmp;
        });
  }

  protected void onNewFinalBlock(BeaconBlock finalBlock) {
    logger.debug(() -> "New final block: " + finalBlock);
    blockTree.setTopBlock(Feedback.of(finalBlock));
    this.finalBlock = finalBlock;
    blockRequests.onNext(createBlockRequests());
  }

  protected void onInvalidBlock(BeaconBlock block) {
    logger.warn("Invalid block received: " + block);
  }

  protected void onNewBlock(Feedback<BeaconBlock> block) {
    block.getFeedback().whenComplete((v,t) -> {
      if (t != null) {
        onInvalidBlock(block.get());
      }
    });

    logger.trace(() -> String.format("Adding block %s to the tree", block.get()));
    blockTree.addBlock(block).forEach(readyBlocks::onNext);
  }

  @Override
  public Disposable subscribeToFinalBlocks(Flux<BeaconBlock> finalBlockRootStream) {
    return Flux.from(finalBlockRootStream).subscribe(this::onNewFinalBlock);
  }

  @Override
  public Disposable subscribeToNewBlocks(Publisher<Feedback<List<BeaconBlock>>> blocksStream) {
    return Flux.from(blocksStream)
        .flatMap(resp -> Flux.fromStream(resp.get().stream().map(resp::delegate)))
        .subscribe(this::onNewBlock);
  }
}
