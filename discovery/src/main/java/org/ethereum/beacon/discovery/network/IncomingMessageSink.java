package org.ethereum.beacon.discovery.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.FluxSink;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Netty interface handler for incoming packets in form of raw bytes data wrapped as {@link
 * BytesValue} Implementation forwards all incoming packets in {@link FluxSink} provided via
 * constructor, so it could be later linked to processor to form incoming messages stream
 */
public class IncomingMessageSink extends SimpleChannelInboundHandler<BytesValue> {
  private static final Logger logger = LogManager.getLogger(IncomingMessageSink.class);
  private final FluxSink<BytesValue> bytesValueSink;

  public IncomingMessageSink(FluxSink<BytesValue> bytesValueSink) {
    this.bytesValueSink = bytesValueSink;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, BytesValue msg) throws Exception {
    logger.trace(() -> String.format("Incoming packet %s in session %s", msg, ctx));
    bytesValueSink.next(msg);
  }
}
