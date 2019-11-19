package org.ethereum.beacon.discovery.message.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.NodeRecordInfo;
import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.FindNodeMessage;
import org.ethereum.beacon.discovery.message.NodesMessage;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.storage.NodeBucket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FindNodeHandler implements MessageHandler<FindNodeMessage> {
  private static final Logger logger = LogManager.getLogger(FindNodeHandler.class);
  private static final int MAX_NODES_PER_MESSAGE = 12;

  public FindNodeHandler() {}

  @Override
  public void handle(FindNodeMessage message, NodeSession session) {
    int start =
        message.getDistance() == 0 ? 0 : 1; // home node  from 0 should be returned only for 0
    List<NodeBucket> nodeBuckets =
        IntStream.range(start, message.getDistance())
            .mapToObj(session::getBucket)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    logger.trace(
        () ->
            String.format(
                "Sending %s nodeBuckets in reply to request in session %s",
                nodeBuckets.size(), session));
    List<List<NodeRecord>> nodeRecordsList = new ArrayList<>();
    int total = 0;

    // Repack to lists of MAX_NODES_PER_MESSAGE size
    for (NodeBucket nodeBucket : nodeBuckets) {
      for (NodeRecordInfo nodeRecordInfo : nodeBucket.getNodeRecords()) {
        if (total % MAX_NODES_PER_MESSAGE == 0) {
          nodeRecordsList.add(new ArrayList<>());
        }
        List<NodeRecord> currentList = nodeRecordsList.get(nodeRecordsList.size() - 1);
        currentList.add(nodeRecordInfo.getNode());
        ++total;
      }
    }

    // Send
    if (nodeRecordsList.isEmpty()) {
      nodeRecordsList.add(Collections.emptyList());
    }
    int finalTotal = total;
    nodeRecordsList.forEach(
        recordsList ->
            session.sendOutgoing(
                MessagePacket.create(
                    session.getHomeNodeId(),
                    session.getNodeRecord().getNodeId(),
                    session.getAuthTag().get(),
                    session.getInitiatorKey(),
                    DiscoveryV5Message.from(
                        new NodesMessage(
                            message.getRequestId(),
                            finalTotal,
                            () -> recordsList,
                            recordsList.size())))));
  }
}
