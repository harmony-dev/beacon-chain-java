package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.message.NodesMessage;
import org.ethereum.beacon.discovery.message.PongMessage;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.storage.NodeTable;
import org.ethereum.beacon.util.ExpirationScheduler;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.ethereum.beacon.discovery.NodeContext.DEFAULT_DISTANCE;

public class DiscoveryV5MessageHandler implements DiscoveryMessageHandler<DiscoveryV5Message> {
  private static final int CLEANUP_DELAY_SECONDS = 60;
  private static final int MAX_NODES_IN_MSG = 24;
  private final Bytes32 homeNodeId;
  private final BytesValue initiatorKey;
  private final BytesValue authTag;
  private final NodeTable nodeTable;
  private ExpirationScheduler<BytesValue> nodeExpirationScheduler =
      new ExpirationScheduler<>(CLEANUP_DELAY_SECONDS, TimeUnit.SECONDS);
  private Map<BytesValue, Integer> nodesCounters = new ConcurrentHashMap<>();
  private Map<BytesValue, ScheduledFuture> nodesExpirationTasks = new ConcurrentHashMap<>();

  public DiscoveryV5MessageHandler(
      Bytes32 homeNodeId, BytesValue initiatorKey, BytesValue authTag, NodeTable nodeTable) {
    this.homeNodeId = homeNodeId;
    this.initiatorKey = initiatorKey;
    this.authTag = authTag;
    this.nodeTable = nodeTable;
  }

  @Override
  public void handleMessage(DiscoveryV5Message message, NodeContext context) {
    MessageCode code = message.getCode();
    switch (code) {
      case PING:
        {
          PongMessage responseMessage =
              new PongMessage(
                  message.getRequestId(),
                  context.getNodeRecord().getSeqNumber(),
                  context.getNodeRecord().getIpV4address(),
                  context.getNodeRecord().getUdpPort());
          context.addOutgoingEvent(
              MessagePacket.create(
                  homeNodeId,
                  context.getNodeRecord().getNodeId(),
                  authTag,
                  initiatorKey,
                  DiscoveryV5Message.from(responseMessage)));
          break;
        }
      case PONG:
        {
          // FIXME: do we need to update NodeRecord from PONG response?
          context.clearRequestId(message.getRequestId(), MessageCode.PING);
          break;
        }
      case FINDNODE:
        {
          List<NodeRecordInfo> nodes =
              nodeTable.findClosestNodes(context.getNodeRecord().getNodeId(), DEFAULT_DISTANCE);
          final AtomicInteger counter = new AtomicInteger();
          nodes.stream()
              .map(NodeRecordInfo::getNode)
              .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / MAX_NODES_IN_MSG))
              .values()
              .forEach(
                  c ->
                      context.addOutgoingEvent(
                          MessagePacket.create(
                              homeNodeId,
                              context.getNodeRecord().getNodeId(),
                              authTag,
                              initiatorKey,
                              DiscoveryV5Message.from(
                                  new NodesMessage(
                                      message.getRequestId(),
                                      nodes.size() / MAX_NODES_IN_MSG,
                                      () -> c,
                                      nodes.size())))));
          break;
        }
      case NODES:
        {
          NodesMessage nodesMessage = (NodesMessage) message.create();
          // NODES total count handling + cleanup schedule
          if (nodesCounters.containsKey(nodesMessage.getRequestId())) {
            synchronized (this) {
              int counter = nodesCounters.get(nodesMessage.getRequestId()) - 1;
              if (counter == 0) {
                cleanUp(nodesMessage.getRequestId(), context);
              } else {
                nodesCounters.put(nodesMessage.getRequestId(), counter);
                updateExpiration(nodesMessage.getRequestId(), context);
              }
            }
          } else if (nodesMessage.getTotal() > 1) {
            nodesCounters.put(nodesMessage.getRequestId(), nodesMessage.getTotal() - 1);
            updateExpiration(nodesMessage.getRequestId(), context);
          } else {
            context.clearRequestId(nodesMessage.getRequestId(), MessageCode.FINDNODE);
          }

          // Parse node records
          nodesMessage
              .getNodeRecords()
              .forEach(
                  nodeRecordV5 -> {
                    if (!nodeTable.getNode(nodeRecordV5.getNodeId()).isPresent()) {
                      // TODO: should we update?
                      nodeTable.save(NodeRecordInfo.createDefault(nodeRecordV5));
                    }
                  });

          break;
        }
      default:
        {
          throw new RuntimeException("Not implemented yet");
        }
    }
  }

  private synchronized void updateExpiration(BytesValue requestId, NodeContext context) {
    nodeExpirationScheduler.put(
        requestId,
        () -> {
          cleanUp(requestId, context);
        });
  }

  private synchronized void cleanUp(BytesValue requestId, NodeContext context) {
    nodesCounters.remove(requestId);
    context.clearRequestId(requestId, MessageCode.FINDNODE);
  }
}
