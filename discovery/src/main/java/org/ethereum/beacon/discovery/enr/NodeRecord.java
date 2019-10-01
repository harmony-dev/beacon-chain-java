package org.ethereum.beacon.discovery.enr;

import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Ethereum Node Record
 *
 * <p>Node record as described in <a href="https://eips.ethereum.org/EIPS/eip-778">EIP-778</a>
 */
public interface NodeRecord {
  // Compressed secp256k1 public key, 33 bytes
  String FIELD_PKEY_SECP256K1 = "secp256k1";
  // IPv4 address
  String FIELD_IP_V4 = "ip";
  // TCP port, integer
  String FIELD_TCP_V4 = "tcp";
  // UDP port, integer
  String FIELD_UDP_V4 = "udp";
  // IPv6 address
  String FIELD_IP_V6 = "ip6";
  // IPv6-specific TCP port
  String FIELD_TCP_V6 = "tcp6";
  // IPv6-specific UDP port
  String FIELD_UDP_V6 = "udp6";

  static NodeRecord fromBase64(String enrBase64) {
    return fromBytes(Base64.getUrlDecoder().decode(enrBase64));
  }

  static NodeRecord fromBytes(byte[] bytes) {
    // record    = [signature, seq, k, v, ...]
    RlpList rlpList = (RlpList) RlpDecoder.decode(bytes).getValues().get(0);
    List<RlpType> values = rlpList.getValues();
    if (values.size() < 4) {
      throw new RuntimeException(
          String.format(
              "Unable to deserialize ENR with less than 4 fields, [%s]", BytesValue.wrap(bytes)));
    }
    RlpString id = (RlpString) values.get(2);
    if (!"id".equals(new String(id.getBytes()))) {
      throw new RuntimeException(
          String.format(
              "Unable to deserialize ENR with no id field at 2-3 records, [%s]",
              BytesValue.wrap(bytes)));
    }

    RlpString idVersion = (RlpString) values.get(3);
    EnrScheme nodeIdentity = EnrScheme.fromString(new String(idVersion.getBytes()));
    if (nodeIdentity == null) {
      throw new RuntimeException(
          String.format(
              "Unknown node identity scheme '%s', couldn't create node record.",
              idVersion.asString()));
    }
    switch (nodeIdentity) {
      case V4:
        {
          return NodeRecordV4.fromRlpList(values);
        }
      default:
        {
          throw new RuntimeException(
              String.format("Builder for identity %s not found", nodeIdentity));
        }
    }
  }

  static NodeRecord fromBytes(BytesValue bytes) {
    return fromBytes(bytes.extractArray());
  }

  /** Every {@link EnrScheme} links with its own implementation */
  EnrScheme getIdentityScheme();

  BytesValue getSignature();

  /**
   * @return The sequence number, a 64-bit unsigned integer. Nodes should increase the number
   *     whenever the record changes and republish the record.
   */
  UInt64 getSeq();

  BytesValue serialize();

  default String asBase64() {
    return new String(Base64.getUrlEncoder().encode(serialize().extractArray()));
  }

  Bytes32 getNodeId();

  /**
   * All optional fields, set varies by identity scheme. `id`, identity scheme is not optional. Most
   * common field names are in constants: {@link #FIELD_IP_V4} etc. (FIELD_*)
   */
  Set<String> getKeys();

  /**
   * @return key value or null, if no field associated with that key. Get keys using {@link
   *     #getKeys()}
   */
  Object getKey(String key);
}
