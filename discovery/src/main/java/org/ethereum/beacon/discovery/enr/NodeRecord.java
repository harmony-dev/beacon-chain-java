package org.ethereum.beacon.discovery.enr;

import org.ethereum.beacon.discovery.IdentityScheme;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.list.BytesValueAccessor;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.type.SSZType;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.Base64;
import java.util.List;

/**
 * Ethereum Node Record
 *
 * <p>Node record as described in <a href="https://eips.ethereum.org/EIPS/eip-778">EIP-778</a>
 */
@SSZSerializable(listAccessor = NodeRecord.NodeRecordAccessor.class)
public interface NodeRecord {

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
    IdentityScheme nodeIdentity = IdentityScheme.fromString(new String(idVersion.getBytes()));
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
      case V5:
        {
          return NodeRecordV5.fromRlpList(values);
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

  /** Every {@link IdentityScheme} links with its own implementation */
  IdentityScheme getIdentityScheme();

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

  class NodeRecordAccessor extends BytesValueAccessor {
    @Override
    public int getChildrenCount(Object value) {
      return ((NodeRecord) value).serialize().size();
    }

    @Override
    public Object getChildValue(Object value, int idx) {
      return ((NodeRecord) value).serialize().get(idx);
    }

    @Override
    public boolean isSupported(SSZField field) {
      return NodeRecord.class.isAssignableFrom(field.getRawClass());
    }

    @Override
    public CompositeInstanceAccessor getInstanceAccessor(SSZField compositeDescriptor) {
      return this;
    }

    @Override
    public ListInstanceBuilder createInstanceBuilder(SSZType sszType) {
      return new SimpleInstanceBuilder() {
        @Override
        protected Object buildImpl(List<Object> children) {
          byte[] vals = new byte[children.size()];
          for (int i = 0; i < children.size(); i++) {
            vals[i] = ((Number) children.get(i)).byteValue();
          }
          BytesValue value = BytesValue.wrap(vals);

          return NodeRecord.fromBytes(value);
        }
      };
    }
  }
}
