package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.discovery.enr.NodeRecordV5;
import org.ethereum.beacon.discovery.enr.NodeStatus;
import org.ethereum.beacon.discovery.storage.NodeTable;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.util.List;
import java.util.function.Predicate;

import static org.ethereum.beacon.discovery.NodeContext.DEFAULT_DISTANCE;
import static org.ethereum.beacon.discovery.enr.NodeRecordV5.NODE_ID_FUNCTION;

public class DiscoverTask {
  public static final int MS_IN_SECOND = 1000;
  NodeTable nodeTable;
  Bytes32 homeNodeId;
  RefreshTask refreshTask;

  public DiscoverTask(NodeTable nodeTable, NodeRecordV5 homeNode, RefreshTask refreshTask) {
    this.nodeTable = nodeTable;
    this.homeNodeId = NODE_ID_FUNCTION.apply(homeNode);
    this.refreshTask = refreshTask;
  }

  // TODO: first batch to include dead and switch them to SLEEP
  public Predicate<NodeRecordInfo> refreshTask() {
    return nodeRecord -> {
      final int DISCOVER_AFTER_SECONDS = 600;
      final int MAX_RETRIES = 10;
      long currentTime = System.currentTimeMillis() / MS_IN_SECOND;
      if (nodeRecord.getStatus() == NodeStatus.ACTIVE
          && nodeRecord.getLastRetry() > currentTime - DISCOVER_AFTER_SECONDS) {
        return false; // no need to rediscover
      }
      if (nodeRecord.getRetry() >= MAX_RETRIES) {
        nodeTable.save(
            new NodeRecordInfo(
                nodeRecord.getNode(), nodeRecord.getLastRetry(), NodeStatus.DEAD, 0));
        return false;
      }
      if ((currentTime - nodeRecord.getLastRetry()) < (nodeRecord.getRetry() * nodeRecord.getRetry())) {
        return false; // too early for retry
      }

      return true;
    };
  }

  public void run() {
    List<NodeRecordInfo> nodes = nodeTable.findClosestNodes(homeNodeId, DEFAULT_DISTANCE);
    nodes.stream()
        .filter(refreshTask())
        .forEach(
            nodeRecord ->
                refreshTask.add(
                    nodeRecord,
                    () -> nodeTable.save(
                        new NodeRecordInfo(
                            nodeRecord.getNode(),
                            System.currentTimeMillis() / MS_IN_SECOND,
                            NodeStatus.ACTIVE,
                            0)),
                    () -> nodeTable.save(
                        new NodeRecordInfo(
                            nodeRecord.getNode(),
                            System.currentTimeMillis() / MS_IN_SECOND,
                            NodeStatus.SLEEP,
                            (nodeRecord.getRetry() + 1)))));
  }
}
