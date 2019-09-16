package org.ethereum.beacon.discovery;

import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.HoleyList;
import org.ethereum.beacon.db.source.SingleValueSource;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.discovery.enr.NodeRecordV5;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class NodeTableImpl implements NodeTable {
  private static final long NUMBER_OF_INDEXES = 256;
  private static final int MAXIMUM_INFO_IN_ONE_BYTE = 256;
  private static final boolean START_FROM_BEGINNING = true;
  private final DataSource<Hash32, NodeRecordInfo> nodeTable;
  private final HoleyList<NodeIndex> indexTable;
  private final Function<NodeRecord, Hash32> nodeKeyFunction;
  private final SingleValueSource<NodeRecordInfo> homeNodeSource;

  public NodeTableImpl(
      DataSource<Hash32, NodeRecordInfo> nodeTable,
      HoleyList<NodeIndex> indexTable,
      SingleValueSource<NodeRecordInfo> homeNodeSource,
      Function<BytesValue, Hash32> hashFunction) {
    this.nodeTable = nodeTable;
    this.indexTable = indexTable;
    this.nodeKeyFunction =
        nodeRecord -> {
          if (!(nodeRecord instanceof NodeRecordV5)) {
            throw new RuntimeException(
                ""); // TODO: exception text
          }
          return hashFunction.apply(((NodeRecordV5) nodeRecord).getPublicKey());
        };
    this.homeNodeSource = homeNodeSource;
  }

  private long getNodeIndex(Bytes32 nodeKey) {
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

    int start =
        START_FROM_BEGINNING
            ? 0
            : nodeKey.size() - activeBytes; // FIXME: check for +-1 error for end index
    BytesValue active = nodeKey.slice(start, activeBytes);
    BigInteger activeNumber = new BigInteger(1, active.extractArray()); // FIXME: is signum ok?
    BigInteger index =
        activeNumber.mod(BigInteger.valueOf(NUMBER_OF_INDEXES)); // FIXME: do we require BI here?

    return index.longValue();
  }

  @Override
  public void save(NodeRecordInfo node) {
    Hash32 nodeKey = nodeKeyFunction.apply(node.getNode());
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
      Optional<NodeIndex> upNodesOptional = indexTable.get(currentIndexUp);
      Optional<NodeIndex> downNodesOptional = indexTable.get(currentIndexDown);
      if (upNodesOptional.isPresent()) {
        NodeIndex upNodes = upNodesOptional.get();
        for (Hash32 currentNodeId : upNodes.getEntries()) {
          if (logDistance(currentNodeId, nodeId) >= logLimit) {
            limitReached = true;
            break;
          } else {
            res.add(getNode(currentNodeId).get());
          }
        }
      }
      if (downNodesOptional.isPresent()) {
        NodeIndex downNodes = downNodesOptional.get();
        for (Hash32 currentNodeId : downNodes.getEntries()) {
          if (logDistance(currentNodeId, nodeId) >= logLimit) {
            limitReached = true;
            break;
          } else {
            res.add(getNode(currentNodeId).get());
          }
        }
      }
      currentIndexUp++; // FIXME: bounds
      currentIndexDown--; // FIXME: bounds
    }

    return new ArrayList<>(res);
  }

  private int logDistance(Bytes32 nodeId1, Bytes32 nodeId2) {
    // TODO: real formula
    return Math.abs((nodeId1.get(0) & 0xFF) - (nodeId2.get(0) & 0xFF));
  }

  @Override
  public NodeRecord getHomeNode() {
    return homeNodeSource.get().map(NodeRecordInfo::getNode).orElse(null);
  }
}
