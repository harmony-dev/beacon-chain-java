package org.ethereum.beacon.discovery;

import com.google.common.base.Objects;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for {@link NodeRecord}. Also saves all necessary data about presence of this node and
 * last test of its availability
 */
public class NodeRecordInfo {
  private static final NodeRecordFactory nodeRecordFactory = NodeRecordFactory.DEFAULT;
  private final NodeRecord node;
  private final Long lastRetry;
  private final NodeStatus status;
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

  public static NodeRecordInfo fromRlpBytes(BytesValue bytes) {
    RlpList internalList = (RlpList) RlpDecoder.decode(bytes.extractArray()).getValues().get(0);
    return new NodeRecordInfo(
        nodeRecordFactory.fromBytes(((RlpString) internalList.getValues().get(0)).getBytes()),
        ((RlpString) internalList.getValues().get(1)).asPositiveBigInteger().longValue(),
        NodeStatus.fromNumber(((RlpString) internalList.getValues().get(2)).getBytes()[0]),
        ((RlpString) internalList.getValues().get(1)).asPositiveBigInteger().intValue());
  }

  public BytesValue toRlpBytes() {
    List<RlpType> values = new ArrayList<>();
    values.add(RlpString.create(getNode().serialize().extractArray()));
    values.add(RlpString.create(getLastRetry()));
    values.add(RlpString.create(getStatus().byteCode()));
    values.add(RlpString.create(getRetry()));
    byte[] bytes = RlpEncoder.encode(new RlpList(values));
    return BytesValue.wrap(bytes);
  }

  public NodeRecord getNode() {
    return (NodeRecord) node;
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

  @Override
  public String toString() {
    return "NodeRecordInfo{"
        + "node="
        + node
        + ", lastRetry="
        + lastRetry
        + ", status="
        + status
        + ", retry="
        + retry
        + '}';
  }
}
