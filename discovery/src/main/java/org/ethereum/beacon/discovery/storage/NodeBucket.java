package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.discovery.NodeRecordInfo;
import org.ethereum.beacon.discovery.NodeStatus;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Storage for nodes, K-Bucket. Holds only {@link #K} nodes, replacing nodes with the same nodeId
 * and nodes with old lastRetry. Also throws out DEAD nodes without taking any notice on other
 * fields.
 */
public class NodeBucket {
  /** Bucket size, number of nodes */
  public static final int K = 16;

  private static final Predicate<NodeRecordInfo> FILTER =
      nodeRecord -> nodeRecord.getStatus().equals(NodeStatus.ACTIVE);
  private final TreeSet<NodeRecordInfo> bucket =
      new TreeSet<>((o1, o2) -> o2.getNode().hashCode() - o1.getNode().hashCode());

  public static NodeBucket fromRlpBytes(BytesValue bytes, NodeRecordFactory nodeRecordFactory) {
    NodeBucket nodeBucket = new NodeBucket();
    ((RlpList) RlpDecoder.decode(bytes.extractArray()).getValues().get(0))
        .getValues().stream()
            .map(rt -> (RlpString) rt)
            .map(RlpString::getBytes)
            .map(BytesValue::wrap)
            .map((BytesValue bytes1) -> NodeRecordInfo.fromRlpBytes(bytes1, nodeRecordFactory))
            .forEach(nodeBucket::put);
    return nodeBucket;
  }

  public synchronized boolean put(NodeRecordInfo nodeRecord) {
    if (FILTER.test(nodeRecord)) {
      if (!bucket.contains(nodeRecord)) {
        boolean modified = bucket.add(nodeRecord);
        if (bucket.size() > K) {
          NodeRecordInfo worst = null;
          for (NodeRecordInfo nodeRecordInfo : bucket) {
            if (worst == null) {
              worst = nodeRecordInfo;
            } else if (worst.getLastRetry() > nodeRecordInfo.getLastRetry()) {
              worst = nodeRecordInfo;
            }
          }
          bucket.remove(worst);
        }
        return modified;
      } else {
        NodeRecordInfo bucketNode = bucket.subSet(nodeRecord, true, nodeRecord, true).first();
        if (nodeRecord.getLastRetry() > bucketNode.getLastRetry()) {
          bucket.remove(bucketNode);
          bucket.add(nodeRecord);
          return true;
        }
      }
    } else {
      return bucket.remove(nodeRecord);
    }

    return false;
  }

  public boolean contains(NodeRecordInfo nodeRecordInfo) {
    return bucket.contains(nodeRecordInfo);
  }

  public void putAll(Collection<NodeRecordInfo> nodeRecords) {
    nodeRecords.forEach(this::put);
  }

  public synchronized BytesValue toRlpBytes() {
    byte[] res =
        RlpEncoder.encode(
            new RlpList(
                bucket.stream()
                    .map(NodeRecordInfo::toRlpBytes)
                    .map(BytesValue::extractArray)
                    .map(RlpString::create)
                    .collect(Collectors.toList())));
    return BytesValue.wrap(res);
  }

  public int size() {
    return bucket.size();
  }

  public List<NodeRecordInfo> getNodeRecords() {
    return new ArrayList<>(bucket);
  }
}
