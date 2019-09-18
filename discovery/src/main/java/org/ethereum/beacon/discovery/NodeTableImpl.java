package org.ethereum.beacon.discovery;

import com.google.common.annotations.VisibleForTesting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.HoleyList;
import org.ethereum.beacon.db.source.SingleValueSource;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class NodeTableImpl implements NodeTable {
  static final long NUMBER_OF_INDEXES = 256;
  private static final Logger logger = LogManager.getLogger(NodeTableImpl.class);
  private static final int MAXIMUM_INFO_IN_ONE_BYTE = 256;
  private static final boolean START_FROM_BEGINNING = true;
  private final DataSource<Hash32, NodeRecordInfo> nodeTable;
  private final HoleyList<NodeIndex> indexTable;
  private final SingleValueSource<NodeRecordInfo> homeNodeSource;

  public NodeTableImpl(
      DataSource<Hash32, NodeRecordInfo> nodeTable,
      HoleyList<NodeIndex> indexTable,
      SingleValueSource<NodeRecordInfo> homeNodeSource) {
    this.nodeTable = nodeTable;
    this.indexTable = indexTable;
    this.homeNodeSource = homeNodeSource;
  }

  @VisibleForTesting
  static long getNodeIndex(Bytes32 nodeKey) {
    int activeBytes = 1;
    long required = NUMBER_OF_INDEXES;
    while (required > 0) {
      if (required == MAXIMUM_INFO_IN_ONE_BYTE) {
        required = 0;
      } else {
        required = required / MAXIMUM_INFO_IN_ONE_BYTE;
      }

      if (required > 0) {
        activeBytes++;
      }
    }

    int start = START_FROM_BEGINNING ? 0 : nodeKey.size() - activeBytes;
    BytesValue active = nodeKey.slice(start, activeBytes);
    BigInteger activeNumber = new BigInteger(1, active.extractArray());
    // XXX: could be optimized for small NUMBER_OF_INDEXES
    BigInteger index = activeNumber.mod(BigInteger.valueOf(NUMBER_OF_INDEXES));

    return index.longValue();
  }

  @Override
  public void save(NodeRecordInfo node) {
    Hash32 nodeKey = Hash32.wrap(node.getNode().getNodeId());
    nodeTable.put(nodeKey, node);
    NodeIndex activeIndex = indexTable.get(getNodeIndex(nodeKey)).orElseGet(NodeIndex::new);
    List<Hash32> nodes = activeIndex.getEntries();
    if (!nodes.contains(nodeKey)) {
      nodes.add(nodeKey);
      indexTable.put(getNodeIndex(nodeKey), activeIndex);
    }
  }

  @Override
  public Optional<NodeRecordInfo> getNode(Bytes32 nodeId) {
    return nodeTable.get(Hash32.wrap(nodeId));
  }

  @Override
  public List<NodeRecordInfo> findClosestNodes(Bytes32 nodeId, int logLimit) {
    long start = getNodeIndex(nodeId);
    boolean limitReached = false;
    long currentIndexUp = start;
    long currentIndexDown = start;
    Set<NodeRecordInfo> res = new HashSet<>();
    while (!limitReached) {
      Optional<NodeIndex> upNodesOptional =
          currentIndexUp >= NUMBER_OF_INDEXES ? Optional.empty() : indexTable.get(currentIndexUp);
      Optional<NodeIndex> downNodesOptional =
          currentIndexDown < 0 ? Optional.empty() : indexTable.get(currentIndexDown);
      if (currentIndexUp >= NUMBER_OF_INDEXES && currentIndexDown < 0) {
        // Bounds are reached from both top and bottom
        break;
      }
      if (upNodesOptional.isPresent()) {
        NodeIndex upNodes = upNodesOptional.get();
        for (Hash32 currentNodeId : upNodes.getEntries()) {
          if (Functions.logDistance(currentNodeId, nodeId) >= logLimit) {
            limitReached = true;
            break;
          } else {
            res.add(getNode(currentNodeId).get());
          }
        }
      }
      if (downNodesOptional.isPresent()) {
        NodeIndex downNodes = downNodesOptional.get();
        List<Hash32> entries = downNodes.getEntries();
        // XXX: iterate in reverse order to reach logDistance limit from the right side
        for (int i = entries.size() - 1; i >= 0; i--) {
          Hash32 currentNodeId = entries.get(i);
          if (Functions.logDistance(currentNodeId, nodeId) >= logLimit) {
            limitReached = true;
            break;
          } else {
            res.add(getNode(currentNodeId).get());
          }
        }
      }
      currentIndexUp++;
      currentIndexDown--;
    }

    return new ArrayList<>(res);
  }

  @Override
  public NodeRecord getHomeNode() {
    return homeNodeSource.get().map(NodeRecordInfo::getNode).orElse(null);
  }
}
