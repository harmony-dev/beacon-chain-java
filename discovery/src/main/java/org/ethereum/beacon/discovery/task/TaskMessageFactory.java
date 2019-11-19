package org.ethereum.beacon.discovery.task;

import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.FindNodeMessage;
import org.ethereum.beacon.discovery.message.PingMessage;
import org.ethereum.beacon.discovery.message.V5Message;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.pipeline.info.FindNodeRequestInfo;
import org.ethereum.beacon.discovery.pipeline.info.RequestInfo;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class TaskMessageFactory {
  public static MessagePacket createPacketFromRequest(
      RequestInfo requestInfo, BytesValue authTag, NodeSession session) {
    switch (requestInfo.getTaskType()) {
      case PING:
        {
          return createPingPacket(authTag, session, requestInfo.getRequestId());
        }
      case FINDNODE:
        {
          FindNodeRequestInfo nodeRequestInfo = (FindNodeRequestInfo) requestInfo;
          return createFindNodePacket(
              authTag, session, requestInfo.getRequestId(), nodeRequestInfo.getDistance());
        }
      default:
        {
          throw new RuntimeException(
              String.format("Type %s is not supported!", requestInfo.getTaskType()));
        }
    }
  }

  public static V5Message createMessageFromRequest(RequestInfo requestInfo, NodeSession session) {
    switch (requestInfo.getTaskType()) {
      case PING:
        {
          return createPing(session, requestInfo.getRequestId());
        }
      case FINDNODE:
        {
          FindNodeRequestInfo nodeRequestInfo = (FindNodeRequestInfo) requestInfo;
          return createFindNode(requestInfo.getRequestId(), nodeRequestInfo.getDistance());
        }
      default:
        {
          throw new RuntimeException(
              String.format("Type %s is not supported!", requestInfo.getTaskType()));
        }
    }
  }

  public static MessagePacket createPingPacket(
      BytesValue authTag, NodeSession session, BytesValue requestId) {

    return MessagePacket.create(
        session.getHomeNodeId(),
        session.getNodeRecord().getNodeId(),
        authTag,
        session.getInitiatorKey(),
        DiscoveryV5Message.from(createPing(session, requestId)));
  }

  public static PingMessage createPing(NodeSession session, BytesValue requestId) {
    return new PingMessage(requestId, session.getNodeRecord().getSeq());
  }

  public static MessagePacket createFindNodePacket(
      BytesValue authTag, NodeSession session, BytesValue requestId, int distance) {
    FindNodeMessage findNodeMessage = createFindNode(requestId, distance);
    return MessagePacket.create(
        session.getHomeNodeId(),
        session.getNodeRecord().getNodeId(),
        authTag,
        session.getInitiatorKey(),
        DiscoveryV5Message.from(findNodeMessage));
  }

  public static FindNodeMessage createFindNode(BytesValue requestId, int distance) {
    return new FindNodeMessage(requestId, distance);
  }
}
