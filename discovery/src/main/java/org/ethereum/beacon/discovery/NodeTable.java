package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.util.List;
import java.util.Optional;

public interface NodeTable {
  void save(NodeRecordInfo node);

  Optional<NodeRecordInfo> getNode(Bytes32 nodeId);

  List<NodeRecordInfo> findClosestNodes(Bytes32 nodeId, int logLimit);

  NodeRecord getHomeNode();
}
