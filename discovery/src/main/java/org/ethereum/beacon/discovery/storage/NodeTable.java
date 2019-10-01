package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.NodeRecordInfo;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.util.List;
import java.util.Optional;

/**
 * Stores Ethereum Node Records in {@link NodeRecordInfo} containers. Also stores home node as node
 * record.
 */
public interface NodeTable {
  void save(NodeRecordInfo node);

  Optional<NodeRecordInfo> getNode(Bytes32 nodeId);

  /** Returns list of nodes including `nodeId` (if it's found) in logLimit distance from it. */
  List<NodeRecordInfo> findClosestNodes(Bytes32 nodeId, int logLimit);

  NodeRecord getHomeNode();
}
