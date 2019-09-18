package org.ethereum.beacon.discovery.message.handler;

import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.FindNodeMessage;
import org.ethereum.beacon.discovery.message.NodesMessage;
import org.ethereum.beacon.discovery.packet.MessagePacket;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.ethereum.beacon.discovery.NodeContext.DEFAULT_DISTANCE;

public class FindNodeHandler implements MessageHandler<FindNodeMessage> {
  private final int maxNodesInMsg;

  public FindNodeHandler(int maxNodesInMsg) {
    this.maxNodesInMsg = maxNodesInMsg;
  }

  @Override
  public void handle(FindNodeMessage message, NodeContext context) {
    List<NodeRecordInfo> nodes =
        context.getNodeTable().findClosestNodes(context.getNodeRecord().getNodeId(), DEFAULT_DISTANCE);
    final AtomicInteger counter = new AtomicInteger();
    nodes.stream()
        .map(NodeRecordInfo::getNode)
        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / maxNodesInMsg))
        .values()
        .forEach(
            c ->
                context.addOutgoingEvent(
                    MessagePacket.create(
                        context.getHomeNodeId(),
                        context.getNodeRecord().getNodeId(),
                        context.getAuthTag().get(),
                        context.getInitiatorKey(),
                        DiscoveryV5Message.from(
                            new NodesMessage(
                                message.getRequestId(),
                                nodes.size() / maxNodesInMsg,
                                () -> c,
                                nodes.size())))));
  }
}
