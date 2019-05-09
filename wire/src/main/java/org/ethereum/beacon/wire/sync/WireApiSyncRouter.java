package org.ethereum.beacon.wire.sync;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
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
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;

public class WireApiSyncRouter implements WireApiSync {

  private final ReplayProcessor<Consumer<WireApiSync>> tasks = ReplayProcessor.create(64);
  private final FluxSink<Consumer<WireApiSync>> tasksSink = tasks.sink();

  public WireApiSyncRouter(
      Publisher<WireApiSync> addedPeersStream,
      Publisher<WireApiSync> removedPeersStream) {

    // TODO simple unlimited first peer here, need something more smart
    Publisher<WireApiSync> freePeersStream = Flux.from(addedPeersStream)
        .flatMap(api -> Flux.just(api).repeat());

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
