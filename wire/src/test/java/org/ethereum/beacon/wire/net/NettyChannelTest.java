package org.ethereum.beacon.wire.net;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class NettyChannelTest {

  @Test
  public void test1() throws Exception {
    NettyServer nettyServer = new NettyServer(26666);
    NettyClient nettyClient = new NettyClient();

    System.out.println("Starting server...");
    nettyServer.start().await();
    System.out.println("Server started");

    System.out.println("Connecting 1...");
    CompletableFuture<NettyChannel> chFut1 = nettyClient.connect("localhost", 26666);
    NettyChannel ch1 = chFut1.get(5, TimeUnit.SECONDS);
    System.out.println("Client channel 1 created");

    Flux.from(ch1.inboundMessageStream())
        .subscribe(
            msg -> System.out.println("Client channel 1 message: " + msg),
            err -> System.out.println("Client channel 1 error: " + err),
            () -> System.out.println("Client channel 1 closed"));
    ch1.subscribeToOutbound(
        Flux.just(BytesValue.fromHexString("0x1111"))
            .repeat(5)
            .delayElements(Duration.ofMillis(20))
            .doOnNext(bb -> System.out.println("Sending msg from client 1...")));

    Thread.sleep(200);

    CountDownLatch msgLatch = new CountDownLatch(10);
    Flux.from(nettyServer.channelsStream())
        .subscribe(
            nettyChannel -> {
              System.out.println("Server channel created.");
              Flux.from(nettyChannel.inboundMessageStream())
                  .subscribe(
                      msg -> {
                        System.out.println("Server channel message: " + msg);
                        msgLatch.countDown();
                      },
                      err -> System.out.println("Server channel error: " + err),
                      () -> System.out.println("Server channel closed"));
            },
            err -> System.out.println("Server error: " + err),
            () -> System.out.println("Server socket closed"));

    System.out.println("Connecting 2...");
    CompletableFuture<NettyChannel> chFut2 = nettyClient.connect("localhost", 26666);
    NettyChannel ch2 = chFut2.get(5, TimeUnit.SECONDS);
    System.out.println("Client channel 2 created");

    Flux.from(ch2.inboundMessageStream())
        .subscribe(
            msg -> System.out.println("Client channel 2 message: " + msg),
            err -> System.out.println("Client channel 2 error: " + err),
            () -> System.out.println("Client channel 2 closed"));
    ch2.subscribeToOutbound(
        Flux.just(BytesValue.fromHexString("0x2222"))
            .repeat(5)
            .delayElements(Duration.ofMillis(20))
            .doOnNext(bb -> System.out.println("Sending msg from client 2...")));

    System.out.println("Waiting for all messages on the server...");
    Assert.assertTrue(msgLatch.await(2, TimeUnit.SECONDS));
    System.out.println("Stopping server...");
    nettyServer.stop();

    Thread.sleep(1000);
    System.out.println("Complete");
  }
}
