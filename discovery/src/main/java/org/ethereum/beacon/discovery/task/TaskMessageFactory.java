package org.ethereum.beacon.discovery.task;

import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.FindNodeMessage;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.message.PingMessage;
import org.ethereum.beacon.discovery.packet.MessagePacket;

public class TaskMessageFactory {
  public static final int DEFAULT_DISTANCE = 10;

  public static MessagePacket createPingPacket(NodeContext context) {

    return MessagePacket.create(
        context.getHomeNodeId(),
        context.getNodeRecord().getNodeId(),
        context.getAuthTag().get(),
        context.getInitiatorKey(),
        DiscoveryV5Message.from(createPing(context)));
  }

  public static PingMessage createPing(NodeContext context) {
    return new PingMessage(
        context.getNextRequestId(MessageCode.PING), context.getNodeRecord().getSeq());
  }

  public static MessagePacket createFindNodePacket(NodeContext context) {
    FindNodeMessage findNodeMessage = createFindNode(context);
    return MessagePacket.create(
        context.getHomeNodeId(),
        context.getNodeRecord().getNodeId(),
        context.getAuthTag().get(),
        context.getInitiatorKey(),
        DiscoveryV5Message.from(findNodeMessage));
  }

  public static FindNodeMessage createFindNode(NodeContext context) {
    return new FindNodeMessage(context.getNextRequestId(MessageCode.FINDNODE), DEFAULT_DISTANCE);
  }
}
