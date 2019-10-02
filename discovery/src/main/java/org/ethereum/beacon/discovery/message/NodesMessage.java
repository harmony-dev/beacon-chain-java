package org.ethereum.beacon.discovery.message;

import com.google.common.base.Objects;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * NODES is the response to a FINDNODE or TOPICQUERY message. Multiple NODES messages may be sent as
 * responses to a single query.
 */
public class NodesMessage implements V5Message {
  // Unique request id
  private final BytesValue requestId;
  // Total number of responses to the request
  private final Integer total;
  // List of nodes upon request
  private final Supplier<List<NodeRecord>> nodeRecordsSupplier;
  // Size of nodes in current response
  private final Integer nodeRecordsSize;
  private List<NodeRecord> nodeRecords = null;

  public NodesMessage(
      BytesValue requestId,
      Integer total,
      Supplier<List<NodeRecord>> nodeRecordsSupplier,
      Integer nodeRecordsSize) {
    this.requestId = requestId;
    this.total = total;
    this.nodeRecordsSupplier = nodeRecordsSupplier;
    this.nodeRecordsSize = nodeRecordsSize;
  }

  @Override
  public BytesValue getRequestId() {
    return requestId;
  }

  public Integer getTotal() {
    return total;
  }

  public synchronized List<NodeRecord> getNodeRecords() {
    if (nodeRecords == null) {
      this.nodeRecords = nodeRecordsSupplier.get();
    }
    return nodeRecords;
  }

  public Integer getNodeRecordsSize() {
    return nodeRecordsSize;
  }

  @Override
  public BytesValue getBytes() {
    return Bytes1.intToBytes1(MessageCode.PONG.byteCode())
        .concat(
            BytesValue.wrap(
                RlpEncoder.encode(
                    new RlpList(
                        RlpString.create(requestId.extractArray()),
                        RlpString.create(total),
                        new RlpList(
                            getNodeRecords().stream()
                                .map(n -> RlpString.create(n.serialize().extractArray()))
                                .collect(Collectors.toList()))))));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NodesMessage that = (NodesMessage) o;
    return Objects.equal(requestId, that.requestId)
        && Objects.equal(total, that.total)
        && Objects.equal(nodeRecordsSize, that.nodeRecordsSize);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(requestId, total, nodeRecordsSize);
  }

  @Override
  public String toString() {
    return "NodesMessage{"
        + "requestId="
        + requestId
        + ", total="
        + total
        + ", nodeRecordsSize="
        + nodeRecordsSize
        + '}';
  }
}
