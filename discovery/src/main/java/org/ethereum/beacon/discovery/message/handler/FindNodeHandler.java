package org.ethereum.beacon.discovery.message.handler;

import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.NodeRecordInfo;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.FindNodeMessage;
import org.ethereum.beacon.discovery.message.NodesMessage;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.storage.NodeBucket;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FindNodeHandler implements MessageHandler<FindNodeMessage> {

  public FindNodeHandler() {}

  @Override
  public void handle(FindNodeMessage message, NodeSession session) {
    List<NodeBucket> nodeBuckets =
        IntStream.range(0, message.getDistance())
            .mapToObj(session::getBucket)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    nodeBuckets.forEach(
        bucket ->
            session.sendOutgoing(
                MessagePacket.create(
                    session.getHomeNodeId(),
                    session.getNodeRecord().getNodeId(),
                    session.getAuthTag().get(),
                    session.getInitiatorKey(),
                    DiscoveryV5Message.from(
                        new NodesMessage(
                            message.getRequestId(),
                            nodeBuckets.size(),
                            () ->
                                bucket.getNodeRecords().stream()
                                    .map(NodeRecordInfo::getNode)
                                    .collect(Collectors.toList()),
                            bucket.size())))));
  }
}
