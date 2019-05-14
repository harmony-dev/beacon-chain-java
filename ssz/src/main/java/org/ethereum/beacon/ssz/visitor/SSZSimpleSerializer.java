package org.ethereum.beacon.ssz.visitor;

import org.ethereum.beacon.ssz.SSZSerializeException;
import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZCompositeType;
import org.ethereum.beacon.ssz.type.SSZListType;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.ethereum.beacon.ssz.visitor.SSZSimpleDeserializer.BYTES_PER_LENGTH_OFFSET;

public class SSZSimpleSerializer
    implements SSZVisitor<SSZSimpleSerializer.SSZSerializerResult, Object> {
  private static BytesValue serializeLength(long len) {
    return BytesValues.ofUnsignedIntLittleEndian(len);
  }

  @Override
  public SSZSerializerResult visitBasicValue(SSZBasicType type, Object value) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    type.getAccessor().encode(value, type.getTypeDescriptor(), baos);
    return new SSZSerializerResult(
        BytesValue.wrap(baos.toByteArray()), baos.size(), type.isFixedSize());
  }

  @Override
  public SSZSerializerResult visitList(
      SSZListType type, Object param, ChildVisitor<Object, SSZSerializerResult> childVisitor) {

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
  public SSZSerializerResult visitSubList(
      SSZListType type,
      Object param,
      int startIdx,
      int len,
      ChildVisitor<Object, SSZSerializerResult> childVisitor) {
    return visitComposite(type, param, childVisitor, startIdx, len);
  }

  @Override
  public SSZSerializerResult visitComposite(
      SSZCompositeType type,
      Object rawValue,
      ChildVisitor<Object, SSZSerializerResult> childVisitor) {
    return visitComposite(type, rawValue, childVisitor, 0, type.getChildrenCount(rawValue));
  }

  private SSZSerializerResult visitComposite(
      SSZCompositeType type,
      Object rawValue,
      ChildVisitor<Object, SSZSerializerResult> childVisitor,
      int startIdx,
      int len) {

    List<SSZSerializerResult> childSerializations = new ArrayList<>();
    boolean fixedSize = type.isFixedSize();
    for (int i = startIdx; i < startIdx + len; i++) {
      SSZSerializerResult res = childVisitor.apply(i, type.getChild(rawValue, i));
      childSerializations.add(res);
    }

    int currentOffset =
        childSerializations.stream()
            .mapToInt(r -> r.isFixedSize() ? r.serializedBody.size() : BYTES_PER_LENGTH_OFFSET)
            .sum();
    BytesValue composite = BytesValue.EMPTY;

    // Fixed part
    for (SSZSerializerResult s : childSerializations) {
      composite =
          composite.concat(s.isFixedSize() ? s.serializedBody : serializeLength(currentOffset));
      if (!s.isFixedSize()) {
        currentOffset = currentOffset + s.serializedLength;
      }
    }

    // Variable part
    for (SSZSerializerResult s : childSerializations) {
      if (s.isFixedSize()) {
        continue;
      }
      composite = composite.concat(s.serializedBody);
    }

    return new SSZSerializerResult(composite, currentOffset, fixedSize);
  }

  public static class SSZSerializerResult {
    final BytesValue serializedBody;
    final int serializedLength;
    final boolean fixedSize;

    public SSZSerializerResult(BytesValue serializedBody, int serializedLength, boolean fixedSize) {
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
