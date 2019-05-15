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

public class SSZSimpleSerializer implements SSZVisitor<SSZSimpleSerializer.SSZHasherSerializerResult, Object>  {

  public static class SSZHasherSerializerResult {
    BytesValue serializedBody;
    BytesValue serializedLength;

    public SSZHasherSerializerResult(BytesValue serializedBody) {
      this.serializedBody = serializedBody;
      this.serializedLength = BytesValue.EMPTY;
    }

    public SSZHasherSerializerResult(BytesValue serializedBody,
                               BytesValue serializedLength) {
      this.serializedBody = serializedBody;
      this.serializedLength = serializedLength;
    }

    public BytesValue getSerializedBody() {
      return serializedBody;
    }

    public BytesValue getSerializedLength() {
      return serializedLength;
    }

    public BytesValue getSerialized() {
      return BytesValue.concat(getSerializedLength(), getSerializedBody());
    }

    public boolean isFixedSize() {
      return serializedLength.isEmpty();
    }
  }

  @Override
  public SSZHasherSerializerResult visitBasicValue(SSZBasicType type, Object value) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    type.getAccessor().encode(value, type.getTypeDescriptor(), baos);
    BytesValue res = BytesValue.wrap(baos.toByteArray());
    if (!type.isFixedSize()) {
      res = serializeLength(res.size()).concat(res);
    }
    return new SSZHasherSerializerResult(res);
  }

  @Override
  public SSZHasherSerializerResult visitList(SSZListType type, Object param,
                                                           ChildVisitor<Object, SSZHasherSerializerResult> childVisitor) {

    if (type.isVector()) {
      if (type.getChildrenCount(param) != type.getVectorLength()) {
        throw new SSZSerializeException("Vector type length doesn't match actual list length: "
            + type.getVectorLength() + " !=  " + type.getChildrenCount(param) + " for " + type.toStringHelper());
      }
    }
    return visitComposite(type, param, childVisitor);
  }

  @Override
  public SSZHasherSerializerResult visitSubList(SSZListType type, Object param,
                                                              int startIdx, int len, ChildVisitor<Object, SSZHasherSerializerResult> childVisitor) {
    return visitComposite(type, param, childVisitor, startIdx, len);
  }

  @Override
  public SSZHasherSerializerResult visitComposite(SSZCompositeType type, Object rawValue,
                                                                ChildVisitor<Object, SSZHasherSerializerResult> childVisitor) {
    return visitComposite(type, rawValue, childVisitor, 0, type.getChildrenCount(rawValue));
  }

  private SSZHasherSerializerResult visitComposite(SSZCompositeType type, Object rawValue,
                                                                 ChildVisitor<Object, SSZHasherSerializerResult> childVisitor, int startIdx, int len) {
    List<BytesValue> childSerializations = new ArrayList<>();
    boolean fixedSize = type.isFixedSize();
    int length = 0;
    for (int i = startIdx; i < startIdx + len; i++) {
      SSZHasherSerializerResult res = childVisitor.apply(i, type.getChild(rawValue, i));
      childSerializations.add(res.serializedLength);
      childSerializations.add(res.serializedBody);
      fixedSize &= res.isFixedSize();
      length += res.serializedBody.size() + res.serializedLength.size();
    }

    return new SSZHasherSerializerResult(BytesValue.concat(childSerializations),
        fixedSize ? BytesValue.EMPTY : serializeLength(length));
  }

  private BytesValue serializeLength(int len) {
    return BytesValues.ofUnsignedIntLittleEndian(len);
  }
}
