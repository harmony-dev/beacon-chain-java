package org.ethereum.beacon.discovery.network;

import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.enr.EnrField;
import org.ethereum.beacon.discovery.enr.IdentitySchema;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Netty discovery UDP client */
public class NettyDiscoveryClientImpl implements DiscoveryClient {
  private static final int STOPPING_TIMEOUT = 10000;
  private static final Logger logger = LogManager.getLogger(NettyDiscoveryClientImpl.class);
  private AtomicBoolean listen = new AtomicBoolean(false);
  private NioDatagramChannel channel;

  /**
   * Constructs UDP client using
   *
   * @param outgoingStream Stream of outgoing packets, client will forward them to the channel
   * @param channel Nio channel
   */
  public NettyDiscoveryClientImpl(
      Publisher<NetworkParcel> outgoingStream, NioDatagramChannel channel) {
    this.channel = channel;
    Flux.from(outgoingStream)
        .subscribe(
            networkPacket ->
                send(networkPacket.getPacket().getBytes(), networkPacket.getNodeRecord()));
    logger.info("UDP discovery client started");
    listen.set(true);
  }

  @Override
  public void stop() {
    if (listen.get()) {
      logger.info("Stopping discovery client");
      listen.set(false);
      if (channel != null) {
        try {
          channel.close().await(STOPPING_TIMEOUT);
        } catch (InterruptedException ex) {
          logger.error("Failed to stop discovery client", ex);
        }
      }
    } else {
      logger.warn("An attempt to stop already stopping/stopped discovery client");
    }
  }

  @Override
  public void send(BytesValue data, NodeRecord recipient) {
    if (!(recipient.getIdentityScheme().equals(IdentitySchema.V4))) {
      String error =
          String.format(
              "Accepts only V4 version of recipient's node records. Got %s instead", recipient);
      logger.error(error);
      throw new RuntimeException(error);
    }
    InetSocketAddress address;
    try {
      address =
          new InetSocketAddress(
              InetAddress.getByAddress(((Bytes4) recipient.get(EnrField.IP_V4)).extractArray()),
              (int) recipient.get(EnrField.UDP_V4));
    } catch (UnknownHostException e) {
      String error = String.format("Failed to resolve host for node record: %s", recipient);
      logger.error(error);
      throw new RuntimeException(error);
    }
    DatagramPacket packet = new DatagramPacket(Unpooled.copiedBuffer(data.extractArray()), address);
    logger.trace(() -> String.format("Sending packet %s", packet));
    channel.write(packet);
    channel.flush();
  }
}
