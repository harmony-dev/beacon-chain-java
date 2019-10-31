package org.ethereum.beacon.discovery.message;

import com.google.common.base.Objects;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.List;

/**
 * FINDNODE queries for nodes at the given logarithmic distance from the recipient's node ID. The
 * node IDs of all nodes in the response must have a shared prefix length of distance with the
 * recipient's node ID. A request with distance 0 should return the recipient's current record as
 * the only result.
 */
public class FindNodeMessage implements V5Message {
  // Unique request id
  private final BytesValue requestId;
  // The requested log2 distance, a positive integer
  private final Integer distance;

  public FindNodeMessage(BytesValue requestId, Integer distance) {
    this.requestId = requestId;
    this.distance = distance;
  }

  public static FindNodeMessage fromRlp(List<RlpType> rlpList) {
    return new FindNodeMessage(
        BytesValue.wrap(((RlpString) rlpList.get(0)).getBytes()),
        ((RlpString) rlpList.get(1)).asPositiveBigInteger().intValueExact());
  }

  @Override
  public BytesValue getRequestId() {
    return requestId;
  }

  public Integer getDistance() {
    return distance;
  }

  @Override
  public BytesValue getBytes() {
    return Bytes1.intToBytes1(MessageCode.FINDNODE.byteCode())
        .concat(
            BytesValue.wrap(
                RlpEncoder.encode(
                    new RlpList(
                        RlpString.create(requestId.extractArray()), RlpString.create(distance)))));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FindNodeMessage that = (FindNodeMessage) o;
    return Objects.equal(requestId, that.requestId) && Objects.equal(distance, that.distance);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(requestId, distance);
  }

  @Override
  public String toString() {
    return "FindNodeMessage{" + "requestId=" + requestId + ", distance=" + distance + '}';
  }
}
