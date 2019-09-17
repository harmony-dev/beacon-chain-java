package org.ethereum.beacon.discovery.network;

import io.netty.channel.socket.nio.NioDatagramChannel;
import org.ethereum.beacon.schedulers.Scheduler;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Discovery server which listens to the messages according to setup */
public interface DiscoveryServer {
  void start(Scheduler scheduler);

  void stop();

  /** Raw incoming packets stream */
  Publisher<BytesValue> getIncomingPackets();

  /** Provides safe way to use internal datagram channel for other services */
  CompletableFuture<Void> useDatagramChannel(Consumer<NioDatagramChannel> consumer);
}
