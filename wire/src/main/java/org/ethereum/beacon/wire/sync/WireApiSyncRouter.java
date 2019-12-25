package org.ethereum.beacon.wire.sync;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.stream.RxUtil;
import org.ethereum.beacon.util.Utils;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.message.payload.BlockBodiesRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockBodiesResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsResponseMessage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Tracks and aggregates {@link WireApiSync} instances from separate peers
 * This is a pretty simple implementation which just delegates API calls in a round robin fashion
 * When no single delegate api available all calls are queued until any api arises
 */
public class WireApiSyncRouter implements WireApiSync {
  private static final Logger logger = LogManager.getLogger(WireApiSyncRouter.class);

  private final ReplayProcessor<Consumer<WireApiSync>> tasks = ReplayProcessor.create(64);
  private final FluxSink<Consumer<WireApiSync>> tasksSink = tasks.sink();
  private final AtomicInteger pendingTasks = new AtomicInteger();

  public WireApiSyncRouter(
      Publisher<WireApiSync> addedPeersStream,
      Publisher<WireApiSync> removedPeersStream) {

    Flux<WireApiSync> freePeersStream =
        RxUtil.collect(addedPeersStream, removedPeersStream)
            .doOnNext(activePeers -> logger.info("Active APIs count: " + activePeers.size()))
            .switchMap(
                activePeers ->
                    activePeers.isEmpty() ? Flux.never() : Flux.fromIterable(activePeers).repeat(),
                1);

    freePeersStream.zipWith(tasks, 1)
        .doOnNext(p -> pendingTasks.decrementAndGet())
        .subscribe(p -> p.getT2().accept(p.getT1()));
  }

  private <C> CompletableFuture<C> submitAsyncTask(Function<WireApiSync, CompletableFuture<C>> task) {
    CompletableFuture<C> ret = new CompletableFuture<>();
    tasksSink.next(api -> Utils.futureForward(task.apply(api), ret));
    int cnt = pendingTasks.incrementAndGet();
    logger.debug("New task submitted. Pending tasks: " + cnt);
    return ret;
  }

  @Override
  public CompletableFuture<BlockRootsResponseMessage> requestBlockRoots(
      BlockRootsRequestMessage requestMessage) {
    return submitAsyncTask(api -> api.requestBlockRoots(requestMessage));
  }

  @Override
  public CompletableFuture<BlockHeadersResponseMessage> requestBlockHeaders(
      BlockHeadersRequestMessage requestMessage) {
    return submitAsyncTask(api -> api.requestBlockHeaders(requestMessage));
  }

  @Override
  public CompletableFuture<Feedback<BlockBodiesResponseMessage>> requestBlockBodies(
      BlockBodiesRequestMessage requestMessage) {
    return submitAsyncTask(api -> api.requestBlockBodies(requestMessage));
  }

  @Override
  public CompletableFuture<Feedback<List<SignedBeaconBlock>>> requestBlocks(
      BlockRequestMessage requestMessage, ObjectHasher<Hash32> hasher) {
    logger.info("request blocks: {}", requestMessage);
    return submitAsyncTask(api -> api.requestBlocks(requestMessage, hasher));
  }

  @Override
  public CompletableFuture<Feedback<List<SignedBeaconBlock>>> requestRecentBlocks(List<Hash32> blockRoots,
      ObjectHasher<Hash32> hasher) {
    return submitAsyncTask(api -> api.requestRecentBlocks(blockRoots, hasher));
  }
}
