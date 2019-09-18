package org.ethereum.beacon.discovery.message.handler;

import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.FindNodeMessage;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.message.NodesMessage;
import org.ethereum.beacon.discovery.message.PongMessage;
import org.ethereum.beacon.util.ExpirationScheduler;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NodesHandler implements MessageHandler<NodesMessage> {
  private static final int CLEANUP_DELAY_SECONDS = 60;
  private ExpirationScheduler<BytesValue> nodeExpirationScheduler =
      new ExpirationScheduler<>(CLEANUP_DELAY_SECONDS, TimeUnit.SECONDS);
  private Map<BytesValue, Integer> nodesCounters = new ConcurrentHashMap<>();

  @Override
  public void handle(NodesMessage message, NodeContext context) {
    // NODES total count handling + cleanup schedule
    if (nodesCounters.containsKey(message.getRequestId())) {
      synchronized (this) {
        int counter = nodesCounters.get(message.getRequestId()) - 1;
        if (counter == 0) {
          cleanUp(message.getRequestId(), context);
        } else {
          nodesCounters.put(message.getRequestId(), counter);
          updateExpiration(message.getRequestId(), context);
        }
      }
    } else if (message.getTotal() > 1) {
      nodesCounters.put(message.getRequestId(), message.getTotal() - 1);
      updateExpiration(message.getRequestId(), context);
    } else {
      context.clearRequestId(message.getRequestId(), MessageCode.FINDNODE);
    }

    // Parse node records
    message
        .getNodeRecords()
        .forEach(
            nodeRecordV5 -> {
              if (!context.getNodeTable().getNode(nodeRecordV5.getNodeId()).isPresent()) {
                // TODO: should we update-merge?
                context.getNodeTable().save(NodeRecordInfo.createDefault(nodeRecordV5));
              }
            });
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
