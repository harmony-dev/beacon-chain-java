package org.ethereum.beacon.discovery.message;

import org.ethereum.beacon.discovery.IdentityScheme;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.List;

public class DiscoveryV5Message implements DiscoveryMessage {
  private final BytesValue bytes;
  private List<RlpType> payload = null;

  public DiscoveryV5Message(BytesValue bytes) {
    this.bytes = bytes;
  }

  public static DiscoveryV5Message from(V5Message v5Message) {
    return new DiscoveryV5Message(v5Message.getBytes());
  }

  @Override
  public BytesValue getBytes() {
    return bytes;
  }

  @Override
  public IdentityScheme getIdentityScheme() {
    return IdentityScheme.V5;
  }

  public MessageCode getCode() {
    return MessageCode.fromNumber(getBytes().get(0));
  }

  private synchronized void decode() {
    if (payload != null) {
      return;
    }
    this.payload =
        ((RlpList) RlpDecoder.decode(getBytes().slice(1).extractArray()).getValues().get(0))
            .getValues();
  }

  public BytesValue getRequestId() {
    decode();
    return BytesValue.wrap(((RlpString) payload.get(0)).getBytes());
  }

  public V5Message create(NodeRecordFactory nodeRecordFactory) {
    decode();
    MessageCode code = MessageCode.fromNumber(getBytes().get(0));
    switch (code) {
      case PING:
        {
          return PingMessage.fromRlp(payload);
        }
      case PONG:
        {
          return PongMessage.fromRlp(payload);
        }
      case FINDNODE:
        {
          return FindNodeMessage.fromRlp(payload);
        }
      case NODES:
        {
          return NodesMessage.fromRlp(payload, nodeRecordFactory);
        }
      default:
        {
          throw new RuntimeException(
              String.format(
                  "Creation of discovery V5 messages from code %s is not supported", code));
        }
    }
  }

  @Override
  public String toString() {
    return "DiscoveryV5Message{" + "code=" + getCode() + ", bytes=" + getBytes() + '}';
  }
}
