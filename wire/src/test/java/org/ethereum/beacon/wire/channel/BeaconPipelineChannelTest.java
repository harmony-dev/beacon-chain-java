package org.ethereum.beacon.wire.channel;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSync;
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
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconPipelineChannelTest {


  static class SimpleChannel<T>  implements Channel<T> {
    Processor<T, T> in;
    Processor<T, T> out;

    public SimpleChannel(Processor<T, T> in, Processor<T, T> out) {
      this.in = in;
      this.out = out;
    }

    @Override
    public Publisher<T> inboundMessageStream() {
      return Flux.from(in).doOnError(Throwable::printStackTrace);
    }

    @Override
    public void subscribeToOutbound(Publisher<T> outboundMessageStream) {
      Flux.from(outboundMessageStream).doOnError(Throwable::printStackTrace).subscribe(out);
    }
  }

  @Test
  public void simpleTest1() throws Exception {
    WireApiSync dummyServer = new WireApiSync() {
      @Override
      public CompletableFuture<BlockRootsResponseMessage> requestBlockRoots(
          BlockRootsRequestMessage requestMessage) {

        CompletableFuture<BlockRootsResponseMessage> ret = new CompletableFuture<>();
        ret.complete(new BlockRootsResponseMessage(Collections.singletonList(new BlockRootSlot(
            Hash32.ZERO, SlotNumber.of(666)))));
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

    DirectProcessor<Message> _1to2 = DirectProcessor.create();
    DirectProcessor<Message> _2to1 = DirectProcessor.create();

    SSZSerializer sszSerializer = new SSZBuilder().buildSerializer();

    BeaconPipeline peer1Pipeline = new BeaconPipeline(sszSerializer, dummyServer);
    SimpleChannel<Message> peer1Channel = new SimpleChannel<>(_2to1, _1to2);
    peer1Pipeline.createFromMessageChannel(peer1Channel);

    BeaconPipeline peer2Pipeline = new BeaconPipeline(sszSerializer, dummyServer);
    SimpleChannel<Message> peer2Channel = new SimpleChannel<>(_1to2, _2to1);
    peer2Pipeline.createFromMessageChannel(peer2Channel);

    WireApiSync peer2SyncClient = peer2Pipeline.getSyncClient();

    CompletableFuture<BlockRootsResponseMessage> resp = peer2SyncClient
        .requestBlockRoots(new BlockRootsRequestMessage(SlotNumber.ZERO, UInt64.ZERO));
    BlockRootsResponseMessage responseMessage = resp.get(1, TimeUnit.SECONDS);
    System.out.println(responseMessage);
    Assert.assertEquals(SlotNumber.of(666), responseMessage.getRoots().get(0).getSlot());
  }
}
