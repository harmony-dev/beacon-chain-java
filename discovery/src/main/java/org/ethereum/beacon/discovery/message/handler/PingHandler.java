package org.ethereum.beacon.discovery.message.handler;

import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.PingMessage;
import org.ethereum.beacon.discovery.message.PongMessage;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import tech.pegasys.artemis.util.bytes.Bytes4;

public class PingHandler implements MessageHandler<PingMessage> {
  @Override
  public void handle(PingMessage message, NodeContext context) {
    PongMessage responseMessage =
        new PongMessage(
            message.getRequestId(),
            context.getNodeRecord().getSeq(),
            ((Bytes4) context.getNodeRecord().get(NodeRecord.FIELD_IP_V4)),
            (int) context.getNodeRecord().get(NodeRecord.FIELD_UDP_V4));
    context.addOutgoingEvent(
        MessagePacket.create(
            context.getHomeNodeId(),
            context.getNodeRecord().getNodeId(),
            context.getAuthTag().get(),
            context.getInitiatorKey(),
            DiscoveryV5Message.from(responseMessage)));
  }
}
