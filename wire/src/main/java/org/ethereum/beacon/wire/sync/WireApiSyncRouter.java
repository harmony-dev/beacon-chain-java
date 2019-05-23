package org.ethereum.beacon.wire.sync;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import org.ethereum.beacon.stream.RxUtil;
import org.ethereum.beacon.util.Utils;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.message.payload.BlockBodiesRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockBodiesResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsResponseMessage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;

/**
 * Tracks and aggregates {@link WireApiSync} instances from separate peers
 * This is a pretty simple implementation which just delegates API calls in a round robin fashion
 * When no single delegate api available all calls are queued until any api arises
 */
public class WireApiSyncRouter implements WireApiSync {

  private final ReplayProcessor<Consumer<WireApiSync>> tasks = ReplayProcessor.create(64);
  private final FluxSink<Consumer<WireApiSync>> tasksSink = tasks.sink();

  public WireApiSyncRouter(
      Publisher<WireApiSync> addedPeersStream,
      Publisher<WireApiSync> removedPeersStream) {

    Publisher<WireApiSync> freePeersStream =
        RxUtil.collect(addedPeersStream, removedPeersStream)
            .switchMap(
                activePeers ->
                    activePeers.isEmpty() ? Flux.never() : Flux.fromIterable(activePeers).repeat(),
                1);

    Flux.zip(freePeersStream, tasks)
      .subscribe(p -> p.getT2().accept(p.getT1()));
  }

  private <C> CompletableFuture<C> submitAsyncTask(Function<WireApiSync, CompletableFuture<C>> task) {
    CompletableFuture<C> ret = new CompletableFuture<>();
    tasksSink.next(api -> Utils.futureForward(task.apply(api), ret));
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
}
