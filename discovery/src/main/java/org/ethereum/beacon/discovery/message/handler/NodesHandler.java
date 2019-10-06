package org.ethereum.beacon.discovery.message.handler;

import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.NodeRecordInfo;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.message.NodesMessage;
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
  public void handle(NodesMessage message, NodeSession session) {
    // NODES total count handling + cleanup schedule
    if (nodesCounters.containsKey(message.getRequestId())) {
      synchronized (this) {
        int counter = nodesCounters.get(message.getRequestId()) - 1;
        if (counter == 0) {
          cleanUp(message.getRequestId(), session);
        } else {
          nodesCounters.put(message.getRequestId(), counter);
          updateExpiration(message.getRequestId(), session);
        }
      }
    } else if (message.getTotal() > 1) {
      nodesCounters.put(message.getRequestId(), message.getTotal() - 1);
      updateExpiration(message.getRequestId(), session);
    } else {
      session.clearRequestId(message.getRequestId(), MessageCode.FINDNODE);
    }

    // Parse node records
    message
        .getNodeRecords()
        .forEach(
            nodeRecordV5 -> {
              if (!session.getNodeTable().getNode(nodeRecordV5.getNodeId()).isPresent()) {
                // TODO: should we update-merge?
                session.getNodeTable().save(NodeRecordInfo.createDefault(nodeRecordV5));
              }
            });
  }


  private synchronized void updateExpiration(BytesValue requestId, NodeSession session) {
    nodeExpirationScheduler.put(
        requestId,
        () -> {
          cleanUp(requestId, session);
        });
  }

  private synchronized void cleanUp(BytesValue requestId, NodeSession session) {
    nodesCounters.remove(requestId);
    session.clearRequestId(requestId, MessageCode.FINDNODE);
  }
}
