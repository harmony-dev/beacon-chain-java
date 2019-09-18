package org.ethereum.beacon.discovery.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.schedulers.RunnableEx;
import org.ethereum.beacon.schedulers.Scheduler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DiscoveryServerImpl implements NettyDiscoveryServer {
  private static final int RECREATION_TIMEOUT = 5000;
  private static final int STOPPING_TIMEOUT = 10000;
  private static final Logger logger = LogManager.getLogger(DiscoveryServerImpl.class);
  private final ReplayProcessor<BytesValue> incomingPackets = ReplayProcessor.cacheLast();
  private final FluxSink<BytesValue> incomingSink = incomingPackets.sink();
  private final Integer udpListenPort;
  private final String udpListenHost;
  private AtomicBoolean listen = new AtomicBoolean(true);
  private Channel channel;
  private NioDatagramChannel datagramChannel;
  private Set<Consumer<NioDatagramChannel>> datagramChannelUsageQueue = new HashSet<>();

  public DiscoveryServerImpl(Scheduler scheduler, String udpListenHost, Integer udpListenPort) {
    this.udpListenHost = udpListenHost;
    this.udpListenPort = udpListenPort;
  }

  public DiscoveryServerImpl(Bytes4 udpListenHost, Integer udpListenPort) {
    try {
      this.udpListenHost = InetAddress.getByAddress(udpListenHost.extractArray()).getHostAddress();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
    this.udpListenPort = udpListenPort;
  }

  @Override
  public void start(Scheduler scheduler) {
    logger.info(String.format("Starting discovery server on UDP port %s", udpListenPort));
    scheduler.execute(
        new RunnableEx() {
          @Override
          public void run() throws Exception {
            serverLoop();
          }
        });
  }

  private void serverLoop() {
    NioEventLoopGroup group = new NioEventLoopGroup(1);
    try {
      while (listen.get()) {
        Bootstrap b = new Bootstrap();
        b.group(group)
            .channel(NioDatagramChannel.class)
            .handler(
                new ChannelInitializer<NioDatagramChannel>() {
                  @Override
                  public void initChannel(NioDatagramChannel ch) throws Exception {
                    ch.pipeline()
                        .addLast(new DatagramToBytesValue())
                        .addLast(new IncomingMessageSink(incomingSink));
                    synchronized (DiscoveryServerImpl.class) {
                      datagramChannel = ch;
                      datagramChannelUsageQueue.forEach(
                          nioDatagramChannelConsumer -> nioDatagramChannelConsumer.accept(ch));
                    }
                  }
                });

        channel = b.bind(udpListenHost, udpListenPort).sync().channel();
        channel.closeFuture().sync();

        if (!listen.get()) {
          logger.info("Shutting down discovery server");
          break;
        }
        logger.error("Discovery server closed. Trying to restore after %s seconds delay");
        Thread.sleep(RECREATION_TIMEOUT);
      }
    } catch (Exception e) {
      logger.error("Can't start discovery server", e);
    } finally {
      try {
        group.shutdownGracefully().sync();
      } catch (Exception ex) {
        logger.error("Failed to shutdown discovery sever thread group", ex);
      }
    }
  }

  @Override
  public Publisher<BytesValue> getIncomingPackets() {
    return incomingPackets;
  }

  public synchronized CompletableFuture<Void> useDatagramChannel(
      Consumer<NioDatagramChannel> consumer) {
    CompletableFuture<Void> usage = new CompletableFuture<>();
    if (datagramChannel != null) {
      consumer.accept(datagramChannel);
      usage.complete(null);
    } else {
      datagramChannelUsageQueue.add(
          nioDatagramChannel -> {
            consumer.accept(nioDatagramChannel);
            usage.complete(null);
          });
    }

    return usage;
  }

  @Override
  public void stop() {
    if (listen.get()) {
      logger.info("Stopping discovery server");
      listen.set(false);
      if (channel != null) {
        try {
          channel.close().await(STOPPING_TIMEOUT);
        } catch (InterruptedException ex) {
          logger.error("Failed to stop discovery server", ex);
        }
      }
    } else {
      logger.warn("An attempt to stop already stopping/stopped discovery server");
    }
  }
}
