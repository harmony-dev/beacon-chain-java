package org.ethereum.beacon.discovery.message;

import org.ethereum.beacon.discovery.IdentityScheme;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordV4;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;
import java.util.stream.Collectors;

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

  public V5Message create() {
    decode();
    MessageCode code = MessageCode.fromNumber(getBytes().get(0));
    switch (code) {
      case PING:
        {
          return new PingMessage(
              BytesValue.wrap(((RlpString) payload.get(0)).getBytes()),
              ((RlpString) payload.get(1)).asPositiveBigInteger().longValueExact());
        }
      case PONG:
        {
          return new PongMessage(
              BytesValue.wrap(((RlpString) payload.get(0)).getBytes()),
              UInt64.fromBytesBigEndian(
                  Bytes8.leftPad(BytesValue.wrap(((RlpString) payload.get(1)).getBytes()))),
              BytesValue.wrap(((RlpString) payload.get(2)).getBytes()),
              ((RlpString) payload.get(3)).asPositiveBigInteger().intValueExact());
        }
      case FINDNODE:
        {
          return new FindNodeMessage(
              BytesValue.wrap(((RlpString) payload.get(0)).getBytes()),
              ((RlpString) payload.get(1)).asPositiveBigInteger().intValueExact());
        }
      case NODES:
        {
          RlpList nodeRecords = (RlpList) payload.get(2);
          return new NodesMessage(
              BytesValue.wrap(((RlpString) payload.get(0)).getBytes()),
              ((RlpString) payload.get(1)).asPositiveBigInteger().intValueExact(),
              () ->
                  nodeRecords.getValues().stream()
                      .map(rs -> (NodeRecordV4) (NodeRecord.fromBytes(((RlpString) rs).getBytes())))
                      .collect(Collectors.toList()),
              nodeRecords.getValues().size());
        }
      default:
        {
          throw new RuntimeException(
              String.format(
                  "Creation of discovery V5 messages from code %s is not supported", code));
        }
    }
  }
}
