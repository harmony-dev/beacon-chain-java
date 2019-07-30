package org.ethereum.beacon.ssz.visitor;

import org.ethereum.beacon.ssz.access.SSZUnionAccessor.UnionInstanceAccessor;
import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZCompositeType;
import org.ethereum.beacon.ssz.type.list.SSZListType;
import org.ethereum.beacon.ssz.type.SSZUnionType;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.ethereum.beacon.ssz.visitor.SosDeserializer.BYTES_PER_LENGTH_OFFSET;

/** SSZ serializer with offset-based encoding of variable sized elements */
public class SosSerializer implements SSZVisitor<SosSerializer.SerializerResult, Object> {

  private static BytesValue serializeLength(long len) {
    return BytesValues.ofUnsignedIntLittleEndian(len);
  }

  @Override
  public SerializerResult visitBasicValue(SSZBasicType type, Object value) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    type.getAccessor().encode(value, type.getTypeDescriptor(), baos);
    return new SerializerResult(BytesValue.wrap(baos.toByteArray()), type.isFixedSize());
  }

  @Override
  public SerializerResult visitList(
      SSZListType type, Object param, ChildVisitor<Object, SerializerResult> childVisitor) {
    return visitComposite(type, param, childVisitor);
  }

  @Override
  public SerializerResult visitSubList(
      SSZListType type,
      Object param,
      int startIdx,
      int len,
      ChildVisitor<Object, SerializerResult> childVisitor) {
    return visitComposite(type, param, childVisitor, startIdx, len);
  }

  @Override
  public SerializerResult visitUnion(
      SSZUnionType type, Object param, ChildVisitor<Object, SerializerResult> childVisitor) {
    UnionInstanceAccessor unionInstanceAccessor =
        type.getAccessor().getInstanceAccessor(type.getTypeDescriptor());
    int typeIndex = unionInstanceAccessor.getTypeIndex(param);
    BytesValue typeIndexBytes = serializeLength(typeIndex);
    BytesValue body = BytesValue.EMPTY;
    if (typeIndex > 0 || !type.isNullable()) {
      SerializerResult result =
          childVisitor.apply(typeIndex, unionInstanceAccessor.getChildValue(param, typeIndex));
      body = result.getSerializedBody();
    }
    return new SerializerResult(typeIndexBytes.concat(body), false);
  }

  @Override
  public SerializerResult visitComposite(
      SSZCompositeType type, Object rawValue, ChildVisitor<Object, SerializerResult> childVisitor) {
    return visitComposite(type, rawValue, childVisitor, 0, type.getChildrenCount(rawValue));
  }

  private SerializerResult visitComposite(
      SSZCompositeType type,
      Object rawValue,
      ChildVisitor<Object, SerializerResult> childVisitor,
      int startIdx,
      int len) {

    List<SerializerResult> childSerializations = new ArrayList<>();
    for (int i = startIdx; i < startIdx + len; i++) {
      SerializerResult res = childVisitor.apply(i, type.getChild(rawValue, i));
      childSerializations.add(res);
    }

    // calculating start offset of variable part
    int currentOffset =
        childSerializations.stream()
            .mapToInt(r -> r.isFixedSize() ? r.serializedBody.size() : BYTES_PER_LENGTH_OFFSET)
            .sum();

    List<BytesValue> pieces = new ArrayList<>();

    // Fixed part
    for (SerializerResult s : childSerializations) {
      pieces.add(s.isFixedSize() ? s.getSerializedBody() : serializeLength(currentOffset));
      if (!s.isFixedSize()) {
        currentOffset = currentOffset + s.getSerializedBody().size();
      }
    }

    // Variable part
    for (SerializerResult s : childSerializations) {
      if (!s.isFixedSize()) {
        pieces.add(s.getSerializedBody());
      }
    }

    return new SerializerResult(BytesValue.concat(pieces), type.isFixedSize());
  }

  public static class SerializerResult {
    private final BytesValue serializedBody;
    private final boolean fixedSize;

    public SerializerResult(BytesValue serializedBody, boolean fixedSize) {
      this.serializedBody = serializedBody;
      this.fixedSize = fixedSize;
    }

    public BytesValue getSerializedBody() {
      return serializedBody;
    }

    public boolean isFixedSize() {
      return fixedSize;
    }
  }
}
