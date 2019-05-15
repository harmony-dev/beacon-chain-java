package org.ethereum.beacon.wire.channel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.channel.beacon.BeaconPipeline;
import org.ethereum.beacon.wire.message.Message;
import org.ethereum.beacon.wire.message.payload.BlockBodiesRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockBodiesResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsResponseMessage.BlockRootSlot;
import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconPipelineChannelTest {


  static class SimpleChannel<T>  implements Channel<T> {
    DirectProcessor<T> in;
    DirectProcessor<T> out;
    FluxSink<T> inSink;
    FluxSink<T> outSink;

    public SimpleChannel(DirectProcessor<T> in, DirectProcessor<T> out) {
      this.in = in;
      this.out = out;
      inSink = in.sink();
      outSink = out.sink();
    }

    @Override
    public Publisher<T> inboundMessageStream() {
      return Flux.from(in).doOnError(Throwable::printStackTrace);
    }

    @Override
    public void subscribeToOutbound(Publisher<T> outboundMessageStream) {
      Flux.from(outboundMessageStream).doOnError(Throwable::printStackTrace).subscribe(out);
    }

    public void close() {
      inSink.complete();
      outSink.complete();
    }
  }

  static class DummyWireApiSync implements WireApiSync {

    private final Scheduler scheduler;
    private final Duration responseDuration;

    public DummyWireApiSync(Scheduler scheduler, Duration responseDuration) {
      this.scheduler = scheduler;
      this.responseDuration = responseDuration;
    }

    @Override
    public CompletableFuture<BlockRootsResponseMessage> requestBlockRoots(
        BlockRootsRequestMessage requestMessage) {

      CompletableFuture<BlockRootsResponseMessage> ret = new CompletableFuture<>();
      scheduler.executeWithDelay(
          responseDuration,
          () ->
              ret.complete(
                  new BlockRootsResponseMessage(
                      Collections.singletonList(
                          new BlockRootSlot(Hash32.ZERO, SlotNumber.of(666))))));
      return ret;
    }

    @Override
    public CompletableFuture<BlockHeadersResponseMessage> requestBlockHeaders(
        BlockHeadersRequestMessage requestMessage) {
      return null;
    }

    @Override
    public CompletableFuture<Feedback<BlockBodiesResponseMessage>> requestBlockBodies(
        BlockBodiesRequestMessage requestMessage) {
      return null;
    }
  };

  ;

  @Test
  public void simpleTest1() throws Exception {
    Schedulers schedulers = Schedulers.createDefault();
    DummyWireApiSync dummyServer = new DummyWireApiSync(schedulers.blocking(), Duration.ZERO);

    DirectProcessor<Message> _1to2 = DirectProcessor.create();
    DirectProcessor<Message> _2to1 = DirectProcessor.create();

    SSZSerializer sszSerializer = new SSZBuilder().buildSerializer();

    BeaconPipeline peer1Pipeline =
        new BeaconPipeline(sszSerializer, null, null, dummyServer, schedulers);
    SimpleChannel<Message> peer1Channel = new SimpleChannel<>(_2to1, _1to2);
    peer1Pipeline.initFromMessageChannel(peer1Channel);

    BeaconPipeline peer2Pipeline =
        new BeaconPipeline(sszSerializer, null, null, dummyServer, schedulers);
    SimpleChannel<Message> peer2Channel = new SimpleChannel<>(_1to2, _2to1);
    peer2Pipeline.initFromMessageChannel(peer2Channel);

    WireApiSync peer2SyncClient = peer2Pipeline.getSyncClient();

    CompletableFuture<BlockRootsResponseMessage> resp = peer2SyncClient
        .requestBlockRoots(new BlockRootsRequestMessage(SlotNumber.ZERO, UInt64.ZERO));
    BlockRootsResponseMessage responseMessage = resp.get(1, TimeUnit.SECONDS);
    System.out.println(responseMessage);
    Assert.assertEquals(SlotNumber.of(666), responseMessage.getRoots().get(0).getSlot());
  }

  @Test
  public void closeTest1() throws Exception {

    DummyWireApiSync dummyServer = new DummyWireApiSync(Schedulers.createDefault().blocking(), Duration.ofMillis(50));

    DirectProcessor<Message> _1to2 = DirectProcessor.create();
    DirectProcessor<Message> _2to1 = DirectProcessor.create();

    SSZSerializer sszSerializer = new SSZBuilder().buildSerializer();
    ControlledSchedulers schedulers = Schedulers.createControlled();

    BeaconPipeline peer1Pipeline =
        new BeaconPipeline(sszSerializer, null, null, dummyServer, schedulers);
    SimpleChannel<Message> peer1Channel = new SimpleChannel<>(_2to1, _1to2);
    peer1Pipeline.initFromMessageChannel(peer1Channel);

    BeaconPipeline peer2Pipeline =
        new BeaconPipeline(sszSerializer, null, null, dummyServer, schedulers);
    SimpleChannel<Message> peer2Channel = new SimpleChannel<>(_1to2, _2to1);
    peer2Pipeline.initFromMessageChannel(peer2Channel);

    WireApiSync peer2SyncClient = peer2Pipeline.getSyncClient();

    CountDownLatch closeLatch = new CountDownLatch(10);
    new Thread(() -> {
      try {
        closeLatch.await();
        System.out.println("Closing the channel");
        peer1Channel.close();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }).start();

    List<CompletableFuture<BlockRootsResponseMessage>> futs = new ArrayList<>();

    for (int i = 0; i < 20; i++) {
      System.out.println("Sending request #" + i);
      CompletableFuture<BlockRootsResponseMessage> resp = peer2SyncClient
          .requestBlockRoots(new BlockRootsRequestMessage(SlotNumber.ZERO, UInt64.ZERO));
      futs.add(resp);
      int finalI = i;
      resp.whenComplete(
          (r, t) -> {
            System.out.println("Call #" + finalI + " complete with " + r + ", " + t);
            closeLatch.countDown();
          });
      Thread.sleep(10);
    }

    schedulers.addTime(Duration.ofMinutes(10));

    Assert.assertTrue(futs.stream().allMatch(fut -> fut.isDone()));
    Assert.assertTrue(futs.stream().anyMatch(fut -> fut.isCompletedExceptionally()));
    Assert.assertTrue(futs.stream().anyMatch(fut -> !fut.isCompletedExceptionally()));
  }

  @Test
  public void timeoutTest1() throws Exception {

    DummyWireApiSync dummyServer = new DummyWireApiSync(Schedulers.createDefault().blocking(), Duration.ofDays(100500));

    DirectProcessor<Message> _1to2 = DirectProcessor.create();
    DirectProcessor<Message> _2to1 = DirectProcessor.create();

    SSZSerializer sszSerializer = new SSZBuilder().buildSerializer();
    ControlledSchedulers schedulers = Schedulers.createControlled();

    BeaconPipeline peer1Pipeline =
        new BeaconPipeline(sszSerializer, null, null, dummyServer, schedulers);
    SimpleChannel<Message> peer1Channel = new SimpleChannel<>(_2to1, _1to2);
    peer1Pipeline.initFromMessageChannel(peer1Channel);

    BeaconPipeline peer2Pipeline =
        new BeaconPipeline(sszSerializer, null, null, dummyServer, schedulers);
    SimpleChannel<Message> peer2Channel = new SimpleChannel<>(_1to2, _2to1);
    peer2Pipeline.initFromMessageChannel(peer2Channel);

    WireApiSync peer2SyncClient = peer2Pipeline.getSyncClient();

    System.out.println("Sending request...");
    CompletableFuture<BlockRootsResponseMessage> resp = peer2SyncClient
        .requestBlockRoots(new BlockRootsRequestMessage(SlotNumber.ZERO, UInt64.ZERO));
    resp.whenComplete((r, t) -> System.out.println("Call complete with " + r + ", " + t));

    schedulers.addTime(Duration.ofMillis(500));

    Assert.assertFalse(resp.isDone());

    schedulers.addTime(Duration.ofMinutes(10));

    Assert.assertTrue(resp.isDone());
    Assert.assertTrue(resp.isCompletedExceptionally());
  }
}
