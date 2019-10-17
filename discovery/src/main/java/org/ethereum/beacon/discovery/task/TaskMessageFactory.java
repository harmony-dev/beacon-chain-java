package org.ethereum.beacon.discovery.task;

import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.FindNodeMessage;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.message.PingMessage;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class TaskMessageFactory {
  public static final int DEFAULT_DISTANCE = 10;

  public static MessagePacket createPingPacket(BytesValue authTag, NodeSession session) {

    return MessagePacket.create(
        session.getHomeNodeId(),
        session.getNodeRecord().getNodeId(),
        authTag,
        session.getInitiatorKey(),
        DiscoveryV5Message.from(createPing(session)));
  }

  public static PingMessage createPing(NodeSession session) {
    return new PingMessage(
        session.getNextRequestId(MessageCode.PING), session.getNodeRecord().getSeq());
  }

  public static MessagePacket createFindNodePacket(BytesValue authTag, NodeSession session) {
    FindNodeMessage findNodeMessage = createFindNode(session);
    return MessagePacket.create(
        session.getHomeNodeId(),
        session.getNodeRecord().getNodeId(),
        authTag,
        session.getInitiatorKey(),
        DiscoveryV5Message.from(findNodeMessage));
  }

  public static FindNodeMessage createFindNode(NodeSession session) {
    return new FindNodeMessage(session.getNextRequestId(MessageCode.FINDNODE), DEFAULT_DISTANCE);
  }
}
