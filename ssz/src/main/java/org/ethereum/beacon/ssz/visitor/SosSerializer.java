package org.ethereum.beacon.ssz.visitor;

import org.ethereum.beacon.ssz.SSZSerializeException;
import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZCompositeType;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZType;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.ethereum.beacon.ssz.visitor.SosDeserializer.BYTES_PER_LENGTH_OFFSET;

/**
 * SSZ serializer with offset-based encoding of variable sized elements
 */
public class SosSerializer
    implements SSZVisitor<SosSerializer.SerializerResult, Object> {
  private static BytesValue serializeLength(long len) {
    return BytesValues.ofUnsignedIntLittleEndian(len);
  }

  @Override
  public SerializerResult visitBasicValue(SSZBasicType type, Object value) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    type.getAccessor().encode(value, type.getTypeDescriptor(), baos);
    return new SerializerResult(
        BytesValue.wrap(baos.toByteArray()), baos.size(), type.isFixedSize());
  }

  @Override
  public SerializerResult visitList(
      SSZListType type, Object param, ChildVisitor<Object, SerializerResult> childVisitor) {

    if (type.isVector()) {
      if (type.getChildrenCount(param) != type.getVectorLength()) {
        throw new SSZSerializeException(
            "Vector type length doesn't match actual list length: "
                + type.getVectorLength()
                + " !=  "
                + type.getChildrenCount(param)
                + " for "
                + type.toStringHelper());
      }
    }
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
  public SerializerResult visitComposite(
      SSZCompositeType type,
      Object rawValue,
      ChildVisitor<Object, SerializerResult> childVisitor) {
    return visitComposite(type, rawValue, childVisitor, 0, type.getChildrenCount(rawValue));
  }

  private SerializerResult visitComposite(
      SSZCompositeType type,
      Object rawValue,
      ChildVisitor<Object, SerializerResult> childVisitor,
      int startIdx,
      int len) {

    List<SerializerResult> childSerializations = new ArrayList<>();
    boolean fixedSize = type.isFixedSize();
    for (int i = startIdx; i < startIdx + len; i++) {
      SerializerResult res = childVisitor.apply(i, type.getChild(rawValue, i));
      childSerializations.add(res);
    }

    int currentOffset =
        childSerializations.stream()
            .mapToInt(r -> r.isFixedSize() ? r.serializedBody.size() : BYTES_PER_LENGTH_OFFSET)
            .sum();
    BytesValue composite = BytesValue.EMPTY;

    // Fixed part
    for (SerializerResult s : childSerializations) {
      composite =
          composite.concat(s.isFixedSize() ? s.serializedBody : serializeLength(currentOffset + s.serializedLength));
      if (!s.isFixedSize()) {
        currentOffset = currentOffset + s.serializedLength;
      }
    }

    // Variable part
    for (SerializerResult s : childSerializations) {
      if (s.isFixedSize()) {
        continue;
      }
      composite = composite.concat(s.serializedBody);
    }

    return new SerializerResult(composite, currentOffset, fixedSize);
  }

  public static class SerializerResult {
    final BytesValue serializedBody;
    final int serializedLength;
    final boolean fixedSize;

    public SerializerResult(BytesValue serializedBody, int serializedLength, boolean fixedSize) {
      this.serializedBody = serializedBody;
      this.serializedLength = serializedLength;
      this.fixedSize = fixedSize;
    }

    public BytesValue getSerializedBody() {
      return serializedBody;
    }

    public int getSerializedLength() {
      return serializedLength;
    }

    public boolean isFixedSize() {
      return fixedSize;
    }
  }
}
