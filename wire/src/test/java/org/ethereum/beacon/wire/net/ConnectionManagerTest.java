package org.ethereum.beacon.wire.net;

import io.netty.channel.ConnectTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.wire.channel.Channel;
import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class ConnectionManagerTest {

  class TestClient implements Client<String> {
    List<CompletableFuture<Channel<BytesValue>>> connections = new ArrayList<>();

    @Override
    public CompletableFuture<Channel<BytesValue>> connect(String s) {
      CompletableFuture<Channel<BytesValue>> ret = new CompletableFuture<>();
      connections.add(ret);
      return ret;
    }
  }

  class TestChannel implements Channel<BytesValue> {
    DirectProcessor<BytesValue> data = DirectProcessor.create();

    @Override
    public Publisher<BytesValue> inboundMessageStream() {
      return data;
    }

    @Override
    public void subscribeToOutbound(Publisher<BytesValue> outboundMessageStream) {
    }

    public void close() {
      data.onComplete();
    }
  }

  @Test
  public void test1() {
    List<Channel<BytesValue>> channels = new ArrayList<>();

    TestClient client = new TestClient();
    ControlledSchedulers schedulers = Schedulers.createControlled();
    ConnectionManager<String> manager = new ConnectionManager<>(null, client,
        schedulers.events());
    Flux.from(manager.channelsStream()).subscribe(channels::add);

    manager.addActivePeer("1");
    Assert.assertEquals(1, client.connections.size());
    Assert.assertEquals(0, channels.size());
    schedulers.addTime(Duration.ofSeconds(10));
    Assert.assertEquals(1, client.connections.size());
    Assert.assertEquals(0, channels.size());
    Channel<BytesValue>  testChannel1 = new TestChannel();
    client.connections.get(0).complete(testChannel1);
    Assert.assertEquals(1, channels.size());
    schedulers.addTime(Duration.ofSeconds(10));
    Assert.assertEquals(1, client.connections.size());
    testChannel1.close();
    Assert.assertEquals(1, client.connections.size());
    schedulers.addTime(Duration.ofMillis(500));
    Assert.assertEquals(1, client.connections.size());
    schedulers.addTime(Duration.ofSeconds(10));
    Assert.assertEquals(2, client.connections.size());
    Assert.assertEquals(1, channels.size());

    client.connections.get(1).completeExceptionally(new ConnectTimeoutException());
    Assert.assertEquals(1, channels.size());
    Assert.assertEquals(2, client.connections.size());
    schedulers.addTime(Duration.ofSeconds(10));
    Assert.assertEquals(3, client.connections.size());
    TestChannel testChannel2 = new TestChannel();
    client.connections.get(2).complete(testChannel2);
    Assert.assertEquals(2, channels.size());
    testChannel2.close();

    Assert.assertEquals(3, client.connections.size());
    manager.removeActivePeer("1", true);
    schedulers.addTime(Duration.ofSeconds(10));
    Assert.assertEquals(3, client.connections.size());
  }
}
