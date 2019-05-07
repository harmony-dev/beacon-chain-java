package org.ethereum.beacon.wire.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class NettyChannelInitializer extends ChannelInitializer<NioSocketChannel> {
  private static final int READ_TIMEOUT_SEC = 60;
  private static final Logger logger = LogManager.getLogger(NettyChannelInitializer.class);

  private final Consumer<NettyChannel> activeChannelListener;

  public NettyChannelInitializer(Consumer<NettyChannel> activeChannelListener) {
    this.activeChannelListener = activeChannelListener;
  }

  @Override
  protected void initChannel(NioSocketChannel ch) throws Exception {
    ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(256 * 1024));
    ch.config().setOption(ChannelOption.SO_RCVBUF, 256 * 1024);
    ch.config().setOption(ChannelOption.SO_BACKLOG, 1024);

    ch.pipeline().addFirst(new ReadTimeoutHandler(READ_TIMEOUT_SEC));
    ch.pipeline().addLast(new LengthFieldPrepender(4));
    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
    ch.pipeline().addLast(new NettyChannel(activeChannelListener));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.error("Unexpected error during channel initialization: " + ctx.channel(), cause);
  }
}
