package org.ethereum.beacon.discovery.enr;

import com.google.common.base.Objects;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.discovery.IdentityScheme;
import org.javatuples.Pair;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import tech.pegasys.artemis.util.bytes.Bytes16;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Node record for V5 protocol */
public class NodeRecordV5 implements NodeRecord {
  // id 	name of identity scheme, e.g. “v4”
  private static final IdentityScheme identityScheme = IdentityScheme.V5;
  private static final Function<List<RlpType>, NodeRecordV5> nodeRecordV5Creator =
      fields -> {
        NodeRecordV5.Builder builder =
            NodeRecordV5.Builder.empty()
                .withSignature(BytesValue.wrap(((RlpString) fields.get(0)).getBytes()))
                .withSeq(
                    UInt64.fromBytesBigEndian(
                        Bytes8.leftPad(BytesValue.wrap(((RlpString) fields.get(1)).getBytes()))));
        for (int i = 4; i < fields.size(); i += 2) {
          builder =
              builder.withKeyField(
                  new String(((RlpString) fields.get(i)).getBytes()),
                  (RlpString) fields.get(i + 1));
        }

        return builder.build();
      };
  public static Function<NodeRecordV5, Bytes32> NODE_ID_FUNCTION =
      nodeRecordV5 -> Hashes.sha256(nodeRecordV5.getPublicKey());
  private UInt64 seq;
  // Signature
  private BytesValue signature;
  // optional fields
  private Map<String, Object> fields = new HashMap<>();

  private NodeRecordV5(
      BytesValue publicKey,
      Bytes4 ipV4address,
      Integer tcpPort,
      Integer udpPort,
      Bytes16 ipV6address,
      Integer tcpV6Port,
      Integer udpV6Port,
      UInt64 seq,
      BytesValue signature) {
    fields.put(FIELD_PKEY_SECP256K1, publicKey);
    fields.put(FIELD_IP_V4, ipV4address);
    fields.put(FIELD_TCP_V4, tcpPort);
    fields.put(FIELD_UDP_V4, udpPort);
    fields.put(FIELD_IP_V6, ipV6address);
    fields.put(FIELD_TCP_V6, tcpV6Port);
    fields.put(FIELD_UDP_V6, udpV6Port);
    this.seq = seq;
    this.signature = signature;
  }

  private NodeRecordV5() {}

  public NodeRecordV5(BytesValue bytes) {
    NodeRecord.fromBytes(bytes);
  }

  public static NodeRecordV5 fromValues(
      BytesValue publicKey,
      Bytes4 ipV4address,
      Integer tcpPort,
      Integer udpPort,
      Bytes16 ipV6address,
      Integer tcpV6Port,
      Integer udpV6Port,
      UInt64 seq,
      BytesValue signature) {
    return new NodeRecordV5(
        publicKey,
        ipV4address,
        tcpPort,
        udpPort,
        ipV6address,
        tcpV6Port,
        udpV6Port,
        seq,
        signature);
  }

  public static NodeRecordV5 fromRlpList(List<RlpType> values) {
    return nodeRecordV5Creator.apply(values);
  }

  public IdentityScheme getIdentityScheme() {
    return identityScheme;
  }

  public BytesValue getPublicKey() {
    return fields.containsKey(FIELD_PKEY_SECP256K1)
        ? (BytesValue) fields.get(FIELD_PKEY_SECP256K1)
        : null;
  }

  public void setPublicKey(BytesValue publicKey) {
    fields.put(FIELD_PKEY_SECP256K1, publicKey);
  }

  public Bytes4 getIpV4address() {
    return fields.containsKey(FIELD_IP_V4) ? (Bytes4) fields.get(FIELD_IP_V4) : null;
  }

  public void setIpV4address(Bytes4 ipV4address) {
    fields.put(FIELD_IP_V4, ipV4address);
  }

  public Integer getTcpPort() {
    return fields.containsKey(FIELD_TCP_V4) ? (Integer) fields.get(FIELD_TCP_V4) : null;
  }

  public void setTcpPort(Integer tcpPort) {
    fields.put(FIELD_TCP_V4, tcpPort);
  }

  public Integer getUdpPort() {
    return fields.containsKey(FIELD_UDP_V4) ? (Integer) fields.get(FIELD_UDP_V4) : null;
  }

  public void setUdpPort(Integer udpPort) {
    fields.put(FIELD_UDP_V4, udpPort);
  }

  public Bytes16 getIpV6address() {
    return fields.containsKey(FIELD_IP_V6) ? (Bytes16) fields.get(FIELD_IP_V6) : null;
  }

  public void setIpV6address(Bytes16 ipV6address) {
    fields.put(FIELD_IP_V6, ipV6address);
  }

  public Integer getTcpV6Port() {
    return fields.containsKey(FIELD_TCP_V6) ? (Integer) fields.get(FIELD_TCP_V6) : null;
  }

  public void setTcpV6Port(Integer tcpV6Port) {
    fields.put(FIELD_TCP_V6, tcpV6Port);
  }

  public Integer getUdpV6Port() {
    return fields.containsKey(FIELD_UDP_V6) ? (Integer) fields.get(FIELD_UDP_V6) : null;
  }

  public void setUdpV6Port(Integer udpV6Port) {
    fields.put(FIELD_UDP_V6, udpV6Port);
  }

  public UInt64 getSeq() {
    return seq;
  }

  public void setSeq(UInt64 seq) {
    this.seq = seq;
  }

  @Override
  public BytesValue getSignature() {
    return signature;
  }

  public void setSignature(BytesValue signature) {
    this.signature = signature;
  }

  @Override
  public Map<String, Object> getFields() {
    return fields;
  }

  /** Uses empty 96 bytes signature when no signature is provided\ */
  @Override
  public BytesValue serialize() {
    // content   = [seq, k, v, ...]
    // signature = sign(content)
    // record    = [signature, seq, k, v, ...]
    List<RlpType> values = new ArrayList<>();
    if (getSignature() != null) {
      values.add(RlpString.create(getSignature().extractArray()));
    } else {
      values.add(RlpString.create(Bytes96.ZERO.extractArray())); // FIXME: is it ok?
    }
    values.add(RlpString.create(getSeq().toBI()));
    values.add(RlpString.create("id"));
    values.add(RlpString.create(getIdentityScheme().stringName()));
    for (Map.Entry<String, Object> keyPair : fields.entrySet()) {
      if (keyPair.getValue() == null) {
        continue;
      }
      values.add(RlpString.create(keyPair.getKey()));
      if (keyPair.getValue() instanceof BytesValue) {
        values.add(fromBytes((BytesValue) keyPair.getValue()));
      } else if (keyPair.getValue() instanceof Number) {
        values.add(fromNumber((Number) keyPair.getValue()));
      } else if (keyPair.getValue() == null) {
        values.add(RlpString.create(new byte[0]));
      } else {
        throw new RuntimeException(
            String.format(
                "Couldn't serialize node record field %s with value %s: no serializer found.",
                keyPair.getKey(), keyPair.getValue()));
      }
    }

    byte[] bytes = RlpEncoder.encode(new RlpList(values));
    assert bytes.length <= 300;
    return BytesValue.wrap(bytes);
  }

  private RlpString fromNumber(Number number) {
    if (number instanceof BigInteger) {
      return RlpString.create((BigInteger) number);
    } else if (number instanceof Long) {
      return RlpString.create((Long) number);
    } else if (number instanceof Integer) {
      return RlpString.create((Integer) number);
    } else {
      throw new RuntimeException(
          String.format("Couldn't serialize number %s : no serializer found.", number));
    }
  }

  private RlpString fromBytes(BytesValue bytes) {
    return RlpString.create(bytes.extractArray());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NodeRecordV5 that = (NodeRecordV5) o;
    return Objects.equal(seq, that.seq)
        && Objects.equal(signature, that.signature)
        && Objects.equal(fields, that.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(seq, signature, fields);
  }

  @Override
  public Bytes32 getNodeId() {
    return NODE_ID_FUNCTION.apply(this);
  }

  @Override
  public String toString() {
    return "NodeRecordV5{"
        + "publicKey="
        + fields.get(FIELD_PKEY_SECP256K1)
        + ", ipV4address="
        + fields.get(FIELD_IP_V4)
        + ", udpPort="
        + fields.get(FIELD_UDP_V4)
        + '}';
  }

  public static class Builder {
    private static final Map<String, Function<Pair<Builder, RlpType>, Builder>> fieldFillersV5 =
        new HashMap<>();

    static {
      fieldFillersV5.put(
          FIELD_IP_V4,
          objects ->
              objects
                  .getValue0()
                  .withIpV4Address(BytesValue.wrap(((RlpString) objects.getValue1()).getBytes())));
      fieldFillersV5.put(
          FIELD_PKEY_SECP256K1,
          objects ->
              objects
                  .getValue0()
                  .withSecp256k1(BytesValue.wrap(((RlpString) objects.getValue1()).getBytes())));
      fieldFillersV5.put(
          FIELD_UDP_V4,
          objects ->
              objects
                  .getValue0()
                  .withUdpPort(
                      ((RlpString) objects.getValue1()).asPositiveBigInteger().intValue()));
      fieldFillersV5.put(
          FIELD_TCP_V4,
          objects ->
              objects
                  .getValue0()
                  .withTcpPort(
                      ((RlpString) objects.getValue1()).asPositiveBigInteger().intValue()));
    }

    private Bytes4 ipV4Address;
    private BytesValue secp256k1;
    private Integer tcpPort;
    private Integer udpPort;
    private UInt64 seqNumber;
    private BytesValue signature;

    private Builder() {}

    public static Builder empty() {
      return new Builder();
    }

    public Builder withIpV4Address(BytesValue ipV4Address) {
      this.ipV4Address = Bytes4.wrap(ipV4Address, 0);
      return this;
    }

    public Builder withTcpPort(Integer port) {
      this.tcpPort = port;
      return this;
    }

    public Builder withUdpPort(Integer port) {
      this.udpPort = port;
      return this;
    }

    public Builder withSecp256k1(BytesValue bytes) {
      this.secp256k1 = bytes;
      return this;
    }

    public Builder withSeq(UInt64 seq) {
      this.seqNumber = seq;
      return this;
    }

    public Builder withSignature(BytesValue signature) {
      this.signature = signature;
      return this;
    }

    public Builder withKeyField(String key, RlpString value) {
      Function<Pair<NodeRecordV5.Builder, RlpType>, NodeRecordV5.Builder> fieldFiller =
          fieldFillersV5.get(key);
      if (fieldFiller == null) {
        throw new RuntimeException(String.format("Couldn't find filler for V5 field '%s'", key));
      }
      return fieldFiller.apply(Pair.with(this, value));
    }

    public NodeRecordV5 build() {
      assert seqNumber != null;
      assert secp256k1 != null;

      NodeRecordV5 nodeRecord = new NodeRecordV5();
      nodeRecord.setIpV4address(ipV4Address);
      nodeRecord.setUdpPort(udpPort);
      nodeRecord.setTcpPort(tcpPort);
      nodeRecord.setSeq(seqNumber);
      nodeRecord.setSignature(signature);
      nodeRecord.setPublicKey(secp256k1);
      return nodeRecord;
    }
  }
}
