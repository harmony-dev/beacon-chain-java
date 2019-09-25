package org.ethereum.beacon.discovery.message.handler;

import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.PingMessage;
import org.ethereum.beacon.discovery.message.PongMessage;
import org.ethereum.beacon.discovery.packet.MessagePacket;

public class PingHandler implements MessageHandler<PingMessage> {
  @Override
  public void handle(PingMessage message, NodeContext context) {
    PongMessage responseMessage =
        new PongMessage(
            message.getRequestId(),
            context.getNodeRecord().getSeq(),
            context.getNodeRecord().getIpV4address(),
            context.getNodeRecord().getUdpPort());
    context.addOutgoingEvent(
        MessagePacket.create(
            context.getHomeNodeId(),
            context.getNodeRecord().getNodeId(),
            context.getAuthTag().get(),
            context.getInitiatorKey(),
            DiscoveryV5Message.from(responseMessage)));
  }
}
