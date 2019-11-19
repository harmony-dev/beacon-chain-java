package org.ethereum.beacon.discovery.network;

import io.netty.channel.socket.nio.NioDatagramChannel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Netty-specific extension of {@link DiscoveryServer}. Made to reuse server channel for client. */
public interface NettyDiscoveryServer extends DiscoveryServer {

  /** Reuse Netty server channel with client, so you are able to send packets from the same port */
  CompletableFuture<Void> useDatagramChannel(Consumer<NioDatagramChannel> consumer);
}
