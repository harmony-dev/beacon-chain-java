package org.ethereum.beacon.discovery.message;

import com.google.common.base.Objects;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * PING checks whether the recipient is alive and informs it about the sender's ENR sequence number.
 */
public class PingMessage implements V5Message {
  // Unique request id
  private final BytesValue requestId;
  // Local ENR sequence number of sender
  private final Long enrSeq;

  public PingMessage(BytesValue requestId, Long enrSeq) {
    this.requestId = requestId;
    this.enrSeq = enrSeq;
  }

  @Override
  public BytesValue getRequestId() {
    return requestId;
  }

  public Long getEnrSeq() {
    return enrSeq;
  }

  @Override
  public BytesValue getBytes() {
    return Bytes1.intToBytes1(MessageCode.PING.byteCode())
        .concat(
            BytesValue.wrap(
                RlpEncoder.encode(
                    new RlpList(
                        RlpString.create(requestId.extractArray()), RlpString.create(enrSeq)))));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PingMessage that = (PingMessage) o;
    return Objects.equal(requestId, that.requestId) && Objects.equal(enrSeq, that.enrSeq);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(requestId, enrSeq);
  }

  @Override
  public String toString() {
    return "PingMessage{" + "requestId=" + requestId + ", enrSeq=" + enrSeq + '}';
  }
}
