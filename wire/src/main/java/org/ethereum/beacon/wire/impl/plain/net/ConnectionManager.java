package org.ethereum.beacon.wire.impl.plain.net;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.wire.impl.plain.channel.Channel;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class ConnectionManager<TAddress> {
  private static final Logger logger = LogManager.getLogger(ConnectionManager.class);
  private static final int RECONNECT_TIMEOUT_SECONDS = 1;

  private final Server server;
  private final Client<TAddress> client;
  private final Scheduler rxScheduler;

  private final ReplayProcessor<Channel<BytesValue>> clientConnections = ReplayProcessor.create();
  private final FluxSink<Channel<BytesValue>> clientConnectionsSink = clientConnections.sink();
  private final Set<TAddress> activePeers = Collections.synchronizedSet(new HashSet<>());
  private final Map<TAddress, Channel<?>> activePeerConnections = new ConcurrentHashMap<>();

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
    if (activePeers.contains(peerAddress)) {
      throw new RuntimeException("Already have active peer address: " + peerAddress);
    }
    activePeers.add(peerAddress);

    Flux.just(peerAddress)
        .doOnNext(addr -> logger.info("Connecting to active peer " + peerAddress))
        .map(client::connect)
        .flatMap(Mono::fromFuture, 1, 1)
        .doOnError(t-> logger.info("Couldn't connect to active peer " + peerAddress + ": " + t))
        .doOnNext(ch -> logger.info("Connected to active peer " + peerAddress))
        .doOnNext(ch -> {
          activePeerConnections.put(peerAddress, ch);
          clientConnectionsSink.next(ch);
        })
        .map(Channel::getCloseFuture)
        .onErrorResume(t -> Flux.just(CompletableFuture.completedFuture(null)))
        .flatMap(f -> Mono.fromFuture(f.thenApply(v -> "")), 1, 1)
        .doOnNext(ch -> {
          activePeerConnections.remove(peerAddress);
          logger.info("Disconnected from active peer " + peerAddress);
        })
        .delayElements(Duration.ofSeconds(RECONNECT_TIMEOUT_SECONDS), rxScheduler.toReactor())
        .repeat(() -> activePeers.contains(peerAddress))
        .subscribe();
  }

  public void removeActivePeer(TAddress peerAddress, boolean disconnect) {
    activePeers.remove(peerAddress);
    if (disconnect) {
      Channel<?> channel = activePeerConnections.remove(peerAddress);
      if (channel != null) {
        channel.close();
      }
    }
  }

  public Publisher<Channel<BytesValue>> channelsStream() {
    return Flux.merge(
            server == null ? Flux.empty() : server.channelsStream(),
            client == null ? Flux.empty() :
                clientConnections)
        .doOnNext(ch -> System.out.println(ch));
  }
}
