package org.ethereum.beacon.wire.net;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.wire.channel.Channel;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class ConnectionManager<TAddress> {
  private static final Logger logger = LogManager.getLogger(ConnectionManager.class);
  private static final int RECONNECT_TIMEOUT_SECONDS = 1;

  private final Server server;
  private final Client<TAddress> client;
  private final Scheduler rxScheduler;

  private final DirectProcessor<Channel<BytesValue>> clientConnections = DirectProcessor.create();
  private final FluxSink<Channel<BytesValue>> clientConnectionsSink = clientConnections.sink();
  private final Set<TAddress> activePeers = Collections.synchronizedSet(new HashSet<>());

  public ConnectionManager(Server server, Client<TAddress> client,
      Scheduler rxScheduler) {
    this.server = server;
    this.client = client;
    this.rxScheduler = rxScheduler;
  }

  public CompletableFuture<Channel<BytesValue>> connect(TAddress peerAddress) {
    return client
        .connect(peerAddress)
        .thenApply(
            a -> {
              clientConnectionsSink.next(a);
              return a;
            });
  }

  public void addActivePeer(TAddress peerAddress) {
    activePeers.add(peerAddress);

    Flux.just(peerAddress)
        .doOnNext(addr -> logger.info("Connecting to active peer " + peerAddress))
        .map(client::connect)
        .flatMap(Mono::fromFuture, 1, 1)
        .doOnError(t-> logger.info("Couldn't connect to active peer " + peerAddress + ": " + t))
        .doOnNext(ch -> logger.info("Connected to active peer " + peerAddress))
        .doOnNext(clientConnectionsSink::next)
        .map(Channel::getCloseFuture)
        .onErrorResume(t -> Flux.just(CompletableFuture.completedFuture(null)))
        .flatMap(f -> Mono.fromFuture(f.thenApply(v -> "")), 1, 1)
        .doOnNext(ch -> logger.info("Disconnected from active peer " + peerAddress))
        .delayElements(Duration.ofSeconds(RECONNECT_TIMEOUT_SECONDS), rxScheduler)
        .repeat(() -> activePeers.contains(peerAddress))
        .subscribe();
  }

  public void removeActivePeer(TAddress peerAddress) {
    activePeers.remove(peerAddress);
  }

  public Publisher<Channel<BytesValue>> channelsStream() {
    return Flux.merge(
        server == null ? Flux.empty() : server.channelsStream(),
        client == null ? Flux.empty() : clientConnections);
  }
}
