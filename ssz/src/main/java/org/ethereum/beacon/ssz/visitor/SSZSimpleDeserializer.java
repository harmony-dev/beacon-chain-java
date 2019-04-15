package org.ethereum.beacon.ssz.visitor;

import java.nio.ByteOrder;
import java.util.function.BiFunction;
import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import org.ethereum.beacon.ssz.SSZSerializeException;
import org.ethereum.beacon.ssz.access.SSZCompositeAccessor.CompositeInstanceBuilder;
import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZCompositeType;
import org.ethereum.beacon.ssz.visitor.SSZSimpleDeserializer.DecodeResult;

public class SSZSimpleDeserializer implements SSZVisitor<DecodeResult, Bytes> {
  static final int BYTES_PER_LENGTH_PREFIX = 4;

  public static class DecodeResult {
    public final Object decodedInstance;
    public final int readBytes;

    public DecodeResult(Object decodedInstance, int readBytes) {
      this.decodedInstance = decodedInstance;
      this.readBytes = readBytes;
    }
  }

  @Override
  public DecodeResult visitBasicValue(SSZBasicType sszType, Bytes param) {
    BytesSSZReaderProxy reader = new BytesSSZReaderProxy(param);
    // TODO support basic codecs with variable size
//    int readBytes = sszType.isFixedSize() ? sszType.getSize() : reader.
    return new DecodeResult(sszType.getAccessor().decode(sszType.getTypeDescriptor(),
        reader), sszType.getSize());
  }

  @Override
  public DecodeResult visitComposite(SSZCompositeType type, Bytes param,
      BiFunction<Integer, Bytes, DecodeResult> childVisitor) {
    int pos = 0;
    int size;
    if (type.isVariableSize()) {
      size = deserializeLength(param.slice(0, 4)) + BYTES_PER_LENGTH_PREFIX;
      pos += BYTES_PER_LENGTH_PREFIX;
    } else {
      size = type.getSize();
    }
    int idx = 0;
    CompositeInstanceBuilder instanceBuilder = type.getAccessor()
        .createInstanceBuilder(type.getTypeDescriptor());
    while (pos < size) {
      DecodeResult childRes = childVisitor.apply(idx, param.slice(pos));
      instanceBuilder.setChild(idx, childRes.decodedInstance);
      pos += childRes.readBytes;
      idx++;
    }
    if (pos != size) {
      throw new SSZSerializeException("Error reading serialized composite, expected to read " + size
          + " bytes but read " + pos + " bytes");
    }
    return new DecodeResult(instanceBuilder.build(), pos);
  }

  private int deserializeLength(Bytes lenBytes) {
    return lenBytes.toInt(ByteOrder.LITTLE_ENDIAN);
  }
}
