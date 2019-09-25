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
  // secp256k1 	compressed secp256k1 public key, 33 bytes
  private BytesValue publicKey;
  // ip 	IPv4 address, 4 bytes
  private Bytes4 ipV4address;
  // tcp 	TCP port, big endian integer
  private Integer tcpPort;
  // udp 	UDP port, big endian integer
  private Integer udpPort;
  // ip6 	IPv6 address, 16 bytes
  private Bytes16 ipV6address;
  // tcp6 	IPv6-specific TCP port, big endian integer
  private Integer tcpV6Port;
  // udp6 	IPv6-specific UDP port, big endian integer
  private Integer udpV6Port;
  // seq The sequence number, a 64-bit unsigned integer. Nodes should increase the number whenever
  // the record changes and republish the record.
  private UInt64 seq;
  // Signature
  private BytesValue signature;

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
    this.publicKey = publicKey;
    this.ipV4address = ipV4address;
    this.tcpPort = tcpPort;
    this.udpPort = udpPort;
    this.ipV6address = ipV6address;
    this.tcpV6Port = tcpV6Port;
    this.udpV6Port = udpV6Port;
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
    return publicKey;
  }

  public void setPublicKey(BytesValue publicKey) {
    this.publicKey = publicKey;
  }

  public Bytes4 getIpV4address() {
    return ipV4address;
  }

  public void setIpV4address(Bytes4 ipV4address) {
    this.ipV4address = ipV4address;
  }

  public Integer getTcpPort() {
    return tcpPort;
  }

  public void setTcpPort(Integer tcpPort) {
    this.tcpPort = tcpPort;
  }

  public Integer getUdpPort() {
    return udpPort;
  }

  public void setUdpPort(Integer udpPort) {
    this.udpPort = udpPort;
  }

  public Bytes16 getIpV6address() {
    return ipV6address;
  }

  public void setIpV6address(Bytes16 ipV6address) {
    this.ipV6address = ipV6address;
  }

  public Integer getTcpV6Port() {
    return tcpV6Port;
  }

  public void setTcpV6Port(Integer tcpV6Port) {
    this.tcpV6Port = tcpV6Port;
  }

  public Integer getUdpV6Port() {
    return udpV6Port;
  }

  public void setUdpV6Port(Integer udpV6Port) {
    this.udpV6Port = udpV6Port;
  }

  @Override
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
    if (getPublicKey() != null) {
      values.add(RlpString.create("secp256k1"));
      values.add(RlpString.create(getPublicKey().extractArray()));
    }
    if (getIpV4address() != null) {
      values.add(RlpString.create("ip"));
      values.add(RlpString.create(getIpV4address().extractArray()));
    }
    if (getTcpPort() != null) {
      values.add(RlpString.create("tcp"));
      values.add(RlpString.create(getTcpPort()));
    }
    if (getUdpPort() != null) {
      values.add(RlpString.create("udp"));
      values.add(RlpString.create(getUdpPort()));
    }
    if (getIpV6address() != null) {
      values.add(RlpString.create("ip6"));
      values.add(RlpString.create(getIpV6address().extractArray()));
    }
    if (getTcpV6Port() != null) {
      values.add(RlpString.create("tcp6"));
      values.add(RlpString.create(getTcpV6Port()));
    }
    if (getUdpV6Port() != null) {
      values.add(RlpString.create("udp6"));
      values.add(RlpString.create(getUdpV6Port()));
    }

    byte[] bytes = RlpEncoder.encode(new RlpList(values));
    assert bytes.length <= 300;
    return BytesValue.wrap(bytes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NodeRecordV5 that = (NodeRecordV5) o;
    return Objects.equal(publicKey, that.publicKey)
        && Objects.equal(ipV4address, that.ipV4address)
        && Objects.equal(tcpPort, that.tcpPort)
        && Objects.equal(udpPort, that.udpPort)
        && Objects.equal(ipV6address, that.ipV6address)
        && Objects.equal(tcpV6Port, that.tcpV6Port)
        && Objects.equal(udpV6Port, that.udpV6Port)
        && Objects.equal(seq, that.seq)
        && Objects.equal(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
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

  @Override
  public Bytes32 getNodeId() {
    return NODE_ID_FUNCTION.apply(this);
  }

  @Override
  public String toString() {
    return "NodeRecordV5{"
        + "publicKey="
        + publicKey
        + ", ipV4address="
        + ipV4address
        + ", udpPort="
        + udpPort
        + '}';
  }

  public static class Builder {
    private static final Map<String, Function<Pair<Builder, RlpType>, Builder>> fieldFillersV5 =
        new HashMap<>();

    static {
      fieldFillersV5.put(
          "ip",
          objects ->
              objects
                  .getValue0()
                  .withIpV4Address(BytesValue.wrap(((RlpString) objects.getValue1()).getBytes())));
      fieldFillersV5.put(
          "secp256k1",
          objects ->
              objects
                  .getValue0()
                  .withSecp256k1(BytesValue.wrap(((RlpString) objects.getValue1()).getBytes())));
      fieldFillersV5.put(
          "udp",
          objects ->
              objects
                  .getValue0()
                  .withUdpPort(
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
        throw new RuntimeException(String.format("Couldn't find filler for V4 field '%s'", key));
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
