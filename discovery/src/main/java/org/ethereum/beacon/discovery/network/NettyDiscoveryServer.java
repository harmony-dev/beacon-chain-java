package org.ethereum.beacon.discovery.network;

import io.netty.channel.socket.nio.NioDatagramChannel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface NettyDiscoveryServer extends DiscoveryServer {

  /**
   * Provides safe way to use internal datagram channel for other services. Channel is usually not
   * available on request, instead it will be available at some time in the future. After server
   * starts and some events happened. Return future is fired when it finally happens.
   *
   * @param consumer Consumer which needs Netty datagram channel. For example, UDP client
   *     application could share the channel with the server
   * @return Future which is completed when channel is provided to consumer
   */
  CompletableFuture<Void> useDatagramChannel(Consumer<NioDatagramChannel> consumer);
}
