package org.ethereum.beacon.discovery.network;

import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordV5;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/** Discovery UDP client */
public class DiscoveryClient {
  private static final Logger logger = LogManager.getLogger(DiscoveryClient.class);
  private final NioDatagramChannel channel;

  /**
   * Constructs UDP client using
   *
   * @param channel Netty UDP datagram channel
   * @param outgoingStream Stream of outgoing packets, client will forward them to the channel
   */
  public DiscoveryClient(NioDatagramChannel channel, Publisher<NetworkParcel> outgoingStream) {
    this.channel = channel;
    Flux.from(outgoingStream)
        .subscribe(
            networkPacket ->
                send(networkPacket.getPacket().getBytes(), networkPacket.getNodeRecord()));
  }

  private void send(BytesValue data, NodeRecord recipient) {
    if (!(recipient instanceof NodeRecordV5)) {
      String error =
          String.format(
              "Accepts only V5 versions of recipient's node records. Got %s instead", recipient);
      logger.error(error);
      throw new RuntimeException(error);
    }
    InetSocketAddress address;
    try {
      address =
          new InetSocketAddress(
              InetAddress.getByAddress(((NodeRecordV5) recipient).getIpV4address().extractArray()),
              ((NodeRecordV5) recipient).getUdpPort());
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
