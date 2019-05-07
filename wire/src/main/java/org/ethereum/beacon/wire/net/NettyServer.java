package org.ethereum.beacon.wire.net;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.publisher.UnicastProcessor;

public class NettyServer {
  private static final Logger logger = LogManager.getLogger(NettyServer.class);

  private UnicastProcessor<NettyChannel> channels = UnicastProcessor.create();
  private FluxSink<NettyChannel> channelsSink = channels.sink();
  private final int port;
  private ChannelFuture channelFuture;


  public NettyServer(int port) {
    this.port = port;
  }

  public Publisher<NettyChannel> channelsStream() {
    return channels;
  }

  private void onChannelActive(NettyChannel channel) {
    channelsSink.next(channel);
  }

  public ChannelFuture start() {
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1,
        new ThreadFactoryBuilder().setNameFormat("netty-service-boss-%d").build());
    NioEventLoopGroup workerGroup = new NioEventLoopGroup(16,
        new ThreadFactoryBuilder().setNameFormat("netty-service-worker-%d").build());

    try {
      ServerBootstrap b = new ServerBootstrap();

      b.group(bossGroup, workerGroup);
      b.channel(NioServerSocketChannel.class);

      b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
      b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10 * 1000);

      b.handler(new LoggingHandler());
      b.childHandler(new NettyChannelInitializer(this::onChannelActive));

      channelFuture = b.bind(port);

      channelFuture.addListener((ChannelFutureListener)
          future -> {
            logger.info("Listening for incoming connections, port: " + port);
            try {
              future.get();
            } catch (Exception e) {
              channelsSink.error(e);
              channelsSink.complete();
            }
          });

      channelFuture.channel().closeFuture().addListener(aa -> {
        logger.debug("Incoming port is closed: " + port);
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
        channelsSink.complete();
      });

      return channelFuture;
    } catch (Exception e) {
      logger.debug("Exception: {} ({})", e.getMessage(), e.getClass().getName());
      throw new RuntimeException("Can't bind the port", e);
    }
  }

  public void stop() {
    if (channelFuture == null) {
      throw new IllegalStateException("Not started");
    }
    channelFuture.addListener((ChannelFutureListener) future -> {
              logger.info("Stopping listening on port " + port + "...");
              future.channel().close();
            });
  }
}
