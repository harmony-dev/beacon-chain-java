package org.ethereum.beacon.ssz.visitor;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ethereum.beacon.ssz.SSZCodecResolver;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.type.SSZCodec;
import tech.pegasys.artemis.util.bytes.BytesValue;

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
      return BytesValue.concat(getSerializedBody(), getSerializedLength());
    }

    public boolean isFixedSize() {
      return serializedLength.isEmpty();
    }
  }

  private final SSZCodecResolver codecResolver;

  public SSZSimpleSerializer(SSZCodecResolver codecResolver) {
    this.codecResolver = codecResolver;
  }

  @Override
  public SSZSerializerResult visitBasicValue(SSZField descriptor, Object value) {
    SSZCodec codec = codecResolver.resolveBasicValueCodec(descriptor);
    if (codec == null) {
      throw new SSZSchemeException("No codec found for " + descriptor);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    codec.encode(value, descriptor, baos);
    return new SSZSerializerResult(BytesValue.wrap(baos.toByteArray()));
  }

  @Override
  public SSZSerializerResult visitComposite(SSZCompositeValue value,
      Function<Long, SSZSerializerResult> childVisitor) {

    List<BytesValue> childSerializations = new ArrayList<>();
    boolean fixedSize = !value.getCompositeType().isVariableSize();
    long length = 0;
    for (long i = 0; i < value.getChildCount(); i++) {
      SSZSerializerResult res = childVisitor.apply(i);
      childSerializations.add(res.serializedLength);
      childSerializations.add(res.serializedBody);
      fixedSize &= res.isFixedSize();
      length += res.serializedBody.size() + res.serializedLength.size();
    }

    return new SSZSerializerResult(BytesValue.concat(childSerializations),
        fixedSize ? BytesValue.EMPTY : serializeLength(length));
  }

  private BytesValue serializeLength(long len) {
    throw new UnsupportedOperationException();
  }
}
