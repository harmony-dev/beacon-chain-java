package org.ethereum.beacon.wire.impl.plain.net;

import io.netty.channel.ChannelFuture;
import org.ethereum.beacon.wire.impl.plain.channel.Channel;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Abstract server which accepts inbound connections making bytes {@link Channel}'s from them.
 */
public interface Server extends AutoCloseable {

  /**
   * Stream of connected channels.
   * This publisher should queue and connections made before anyone subscribed and replay them
   * to the first subscriber. The same rule applies to the Channel's inbound bytes
   * (see {@link Channel#inboundMessageStream()})
   */
  Publisher<? extends Channel<BytesValue>> channelsStream();

  /**
   * Start listening for inbound connections
   * @return Future which indicates when ready to accept connections or error
   */
  ChannelFuture start();

  /**
   * Stops listening and release any system resources allocated
   */
  void stop();

  @Override
  default void close() throws Exception {
    stop();
  }
}
