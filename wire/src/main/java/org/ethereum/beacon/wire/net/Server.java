package org.ethereum.beacon.wire.net;

import io.netty.channel.ChannelFuture;
import org.ethereum.beacon.wire.channel.Channel;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface Server extends AutoCloseable {

  Publisher<? extends Channel<BytesValue>> channelsStream();

  ChannelFuture start();

  void stop();

  @Override
  default void close() throws Exception {
    stop();
  }
}
