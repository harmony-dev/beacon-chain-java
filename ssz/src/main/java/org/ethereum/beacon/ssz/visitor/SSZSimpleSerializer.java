package org.ethereum.beacon.ssz.visitor;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ethereum.beacon.ssz.scheme.SSZBasicType;
import org.ethereum.beacon.ssz.scheme.SSZCompositeType;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;

public class SSZSimpleSerializer implements SSZVisitor<SSZSimpleSerializer.SSZSerializerResult> {

  public static class SSZSerializerResult {
    BytesValue serializedBody;
    BytesValue serializedLength;

    public SSZSerializerResult(BytesValue serializedBody) {
      this.serializedBody = serializedBody;
      this.serializedLength = BytesValue.EMPTY;
    }

    public SSZSerializerResult(BytesValue serializedBody,
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
  public SSZSerializerResult visitBasicValue(SSZBasicType type, Object value) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    type.getValueCodec().encode(value, type.getTypeDescriptor(), baos);
    return new SSZSerializerResult(BytesValue.wrap(baos.toByteArray()));
  }

  @Override
  public SSZSerializerResult visitComposite(SSZCompositeType type, Object rawValue,
      Function<Integer, SSZSerializerResult> childVisitor) {

    List<BytesValue> childSerializations = new ArrayList<>();
    boolean fixedSize = type.isFixedSize();
    int length = 0;
    for (int i = 0; i < type.getChildrenCount(rawValue); i++) {
      SSZSerializerResult res = childVisitor.apply(i);
      childSerializations.add(res.serializedLength);
      childSerializations.add(res.serializedBody);
      fixedSize &= res.isFixedSize();
      length += res.serializedBody.size() + res.serializedLength.size();
    }

    return new SSZSerializerResult(BytesValue.concat(childSerializations),
        fixedSize ? BytesValue.EMPTY : serializeLength(length));
  }

  private BytesValue serializeLength(int len) {
    return BytesValues.ofUnsignedIntLittleEndian(len);
  }
}
