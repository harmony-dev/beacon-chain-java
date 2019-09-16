package org.ethereum.beacon.discovery.enr;

import com.google.common.base.Objects;
import org.ethereum.beacon.discovery.NodeStatus;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

/**
 * Container for {@link NodeRecord}. Also saves all necessary data about presence of this node and
 * last test of its availability
 */
@SSZSerializable
public class NodeRecordInfo {
  @SSZ private final NodeRecord node;
  @SSZ private final Long lastRetry;
  @SSZ private final NodeStatus status;

  @SSZ(type = "uint8")
  private final Integer retry;

  public NodeRecordInfo(NodeRecord node, Long lastRetry, NodeStatus status, Integer retry) {
    this.node = node;
    this.lastRetry = lastRetry;
    this.status = status;
    this.retry = retry;
  }

  public static NodeRecordInfo createDefault(NodeRecord nodeRecord) {
    return new NodeRecordInfo(nodeRecord, -1L, NodeStatus.ACTIVE, 0);
  }

  public NodeRecordV5 getNode() {
    return (NodeRecordV5) node;
  }

  public Long getLastRetry() {
    return lastRetry;
  }

  public NodeStatus getStatus() {
    return status;
  }

  public Integer getRetry() {
    return retry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NodeRecordInfo that = (NodeRecordInfo) o;
    return Objects.equal(node, that.node)
        && Objects.equal(lastRetry, that.lastRetry)
        && status == that.status
        && Objects.equal(retry, that.retry);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(node, lastRetry, status, retry);
  }
}
