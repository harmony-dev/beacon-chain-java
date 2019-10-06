package org.ethereum.beacon.discovery.message.handler;

import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.PingMessage;
import org.ethereum.beacon.discovery.message.PongMessage;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import tech.pegasys.artemis.util.bytes.Bytes4;

public class PingHandler implements MessageHandler<PingMessage> {
  @Override
  public void handle(PingMessage message, NodeSession session) {
    PongMessage responseMessage =
        new PongMessage(
            message.getRequestId(),
            session.getNodeRecord().getSeq(),
            ((Bytes4) session.getNodeRecord().get(NodeRecord.FIELD_IP_V4)),
            (int) session.getNodeRecord().get(NodeRecord.FIELD_UDP_V4));
    session.sendOutgoing(
        MessagePacket.create(
            session.getHomeNodeId(),
            session.getNodeRecord().getNodeId(),
            session.getAuthTag().get(),
            session.getInitiatorKey(),
            DiscoveryV5Message.from(responseMessage)));
  }
}
