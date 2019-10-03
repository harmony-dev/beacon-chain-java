package org.ethereum.beacon.wire.impl.plain.net.netty;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.wire.impl.plain.net.Server;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.publisher.UnicastProcessor;

public class NettyServer implements Server {
  private static final Logger logger = LogManager.getLogger(NettyServer.class);

  private UnicastProcessor<NettyChannel> channels = UnicastProcessor.create();
  private Publisher<NettyChannel> channelsDispatcher = createDispatcherProcessor(channels);
  private FluxSink<NettyChannel> channelsSink = channels.sink();
  private final int port;
  private ChannelFuture channelFuture;
  private final NioEventLoopGroup workerGroup;

  /**
   * UnicastProcessor allows single subscriber only.
   * Work around by attaching a DirectProcessor to it.
   */
  @NotNull
  private static Publisher<NettyChannel> createDispatcherProcessor(
      Publisher<NettyChannel> channels) {
    Processor<NettyChannel,NettyChannel> processor = ReplayProcessor.create();
    Flux.from(channels).subscribe(processor);
    return processor;
  }

  public NettyServer(int port, NioEventLoopGroup workerGroup) {
    this.port = port;
    this.workerGroup = workerGroup;
  }

  public NettyServer(int port, Executor executor) {
    this(port, new NioEventLoopGroup(16, executor));
  }

  public NettyServer(int port) {
    this(port, new NioEventLoopGroup(16,
        new ThreadFactoryBuilder().setNameFormat("netty-service-worker-%d").build()));
  }

  @Override
  public Publisher<NettyChannel> channelsStream() {
    return channelsDispatcher;
  }

  private void onChannelActive(NettyChannel channel) {
    channelsSink.next(channel);
  }

  @Override
  public ChannelFuture start() {

    try {
      ServerBootstrap b = new ServerBootstrap();

      b.group(workerGroup, workerGroup);
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
        channelsSink.complete();
      });

      return channelFuture;
    } catch (Exception e) {
      logger.debug("Exception: {} ({})", e.getMessage(), e.getClass().getName());
      throw new RuntimeException("Can't bind the port", e);
    }
  }

  @Override
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
