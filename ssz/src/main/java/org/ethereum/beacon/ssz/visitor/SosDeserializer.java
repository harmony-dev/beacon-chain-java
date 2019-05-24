package org.ethereum.beacon.ssz.visitor;

import net.consensys.cava.bytes.Bytes;
import org.ethereum.beacon.ssz.access.SSZCompositeAccessor.CompositeInstanceBuilder;
import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZCompositeType;
import org.ethereum.beacon.ssz.type.SSZContainerType;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZType;
import org.ethereum.beacon.ssz.visitor.SosDeserializer.DecodeResult;
import org.javatuples.Pair;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.nio.ByteOrder;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SSZ deserializer with offset-based decoding of variable sized elements
 */
public class SosDeserializer implements SSZVisitor<DecodeResult, Pair<Bytes, Boolean>> {
  static final int BYTES_PER_LENGTH_OFFSET = 4;

  /**
   *  Decodes basic value
   *
   * @param sszType     Type of field, should be some basic type
   * @param param       Bytes data / Boolean flag whether to extract body or offset for variable size types.
   *                    If type is with variable size and this flag is set to TRUE, offset is extracted. Otherwise body.
   * @return  DecodeResult
   */
  @Override
  public DecodeResult visitBasicValue(SSZBasicType sszType, Pair<Bytes, Boolean> param) {
    Optional<DecodeResult> offsetLength = extractVariableTypeOffset(sszType, param);
    if (offsetLength.isPresent()) {
      return offsetLength.get();
    }

    SSZReader reader = new SSZReader(param.getValue0());
    if (sszType.isFixedSize()) {
      int readBytes = sszType.getSize();
      return new DecodeResult(
          sszType.getAccessor().decode(sszType.getTypeDescriptor(), reader), readBytes, true);
    } else {
      return new DecodeResult(
          sszType.getAccessor().decode(sszType.getTypeDescriptor(), reader),
          param.getValue0().size(),
          false);
    }
  }

  private Optional<DecodeResult> extractVariableTypeOffset(SSZType sszType, Pair<Bytes, Boolean> param) {
    if (Boolean.TRUE.equals(param.getValue1()) && !sszType.isFixedSize()) {
      return Optional.of(new DecodeResult(
          deserializeLength(param.getValue0().slice(0, BYTES_PER_LENGTH_OFFSET)),
          BYTES_PER_LENGTH_OFFSET,
          false));
    }

    return Optional.empty();
  }

  /** Calculates start offset for variable part based on type scheme */
  private static int calculateFixedPartSize(SSZType type, int level) {
    int res = 0;
    if (type.isList()) {
      SSZListType listType = ((SSZListType) type);
      if (level == 0) {
        return SSZType.VARIABLE_SIZE;
      }
      if (listType.getSize() == SSZType.VARIABLE_SIZE) {
        return BYTES_PER_LENGTH_OFFSET;
      } else {
        return listType.getSize();
      }
    } else if (type.isContainer()) {
      for (SSZType sszType : ((SSZContainerType) type).getChildTypes()) {
        res += calculateFixedPartSize(sszType, level + 1);
      }
    } else if (type.isBasicType()) {
      if (type.isFixedSize()) {
        return type.getSize();
      } else {
        return BYTES_PER_LENGTH_OFFSET;
      }
    } else {
      throw new RuntimeException("Unexpected type: " + type);
    }
    return res;
  }

  private static int calculateListFixedPartSize(SSZType type, Bytes data) {
    assert type instanceof SSZListType;
    SSZListType listType = ((SSZListType) type);
    if (data.isEmpty()) {
      return 0;
    }
    if (listType.getElementType().isFixedSize()) {
      return data.size();
    } else {
      int lastOffset = -1;
      int pos = 0;
      while (pos <= data.size()) {
        lastOffset = deserializeLength(data.slice(pos, BYTES_PER_LENGTH_OFFSET));
        pos += BYTES_PER_LENGTH_OFFSET;
        if (lastOffset == data.size()) {
          return pos;
        } else if (lastOffset > data.size()) {
          throw new RuntimeException(String.format("Incorrectly encoded data. Offset %s is greater than the size of list (%s)", lastOffset, data.size()));
        }
      }
      throw new RuntimeException("End of data reached while end offset not found");
    }
  }

  private static int calculateFixedPartSize(SSZType type, Bytes data) {
    int fixedPartSize = calculateFixedPartSize(type, 0);
    if (fixedPartSize == SSZType.VARIABLE_SIZE) {
      fixedPartSize = calculateListFixedPartSize(type, data);
    }

    return fixedPartSize;
  }

  /**
   * Decodes composite value
   *
   * @param type        Type of field, should be some composite type
   * @param param       Bytes data / Boolean flag whether to extract body or offset for variable size types.
   *                    If type is with variable size and this flag is set to TRUE, offset is extracted. Otherwise body.
   * @param childVisitor  Visitor which will be used for children
   * @return DecodeResult
   */
  @Override
  public DecodeResult visitComposite(
      SSZCompositeType type,
      Pair<Bytes, Boolean> param,
      ChildVisitor<Pair<Bytes, Boolean>, DecodeResult> childVisitor) {
    Optional<DecodeResult> offsetLength = extractVariableTypeOffset(type, param);
    if (offsetLength.isPresent()) {
      return offsetLength.get();
    }

    int fixedPos = 0;
    AtomicInteger variablePartConsumed = new AtomicInteger(0);
    int idx = 0;
    CompositeInstanceBuilder instanceBuilder =
        type.getAccessor().createInstanceBuilder(type.getTypeDescriptor());
    boolean isFixedSize = true;
    int maxIndex = calcTypeMaxIndex(type);
    int startOffset = calculateFixedPartSize(type, param.getValue0());
    int fixedPartEnd = startOffset;
    while (fixedPos < param.getValue0().size() && idx < maxIndex && fixedPos < fixedPartEnd) {
      DecodeResult childRes =
          childVisitor.apply(idx, Pair.with(param.getValue0().slice(fixedPos), true));
      if (childRes.isFixedSize) {
        instanceBuilder.setChild(idx, childRes.decodedInstance);
      } else {
        isFixedSize = false;
        int endOffset = (Integer) childRes.decodedInstance;
        DecodeResult res =
            childVisitor.apply(
                idx, Pair.with(param.getValue0().slice(startOffset, endOffset - startOffset), false));
        startOffset = endOffset;
        variablePartConsumed.addAndGet(res.readBytes);
        instanceBuilder.setChild(idx, res.decodedInstance);
      }
      fixedPos += childRes.readBytes;
      idx++;
    }
    fixedPos += variablePartConsumed.get();

    return new DecodeResult(instanceBuilder.build(), fixedPos, isFixedSize);
  }

  private int calcTypeMaxIndex(SSZType type) {
    int maxIndex = Integer.MAX_VALUE;
    if (type instanceof SSZContainerType) {
      maxIndex = ((SSZContainerType) type).getChildTypes().size();
    } else if (type instanceof SSZListType && ((SSZListType) type).isVector()) {
      maxIndex = ((SSZListType) type).getVectorLength();
    }

    return maxIndex;
  }

  private static int deserializeLength(Bytes lenBytes) {
    return lenBytes.toInt(ByteOrder.LITTLE_ENDIAN);
  }

  public static class DecodeResult {
    public final Object decodedInstance;
    public final int readBytes;
    public final boolean isFixedSize;

    public DecodeResult(Object decodedInstance, int readBytes, boolean isFixedSize) {
      this.decodedInstance = decodedInstance;
      this.readBytes = readBytes;
      this.isFixedSize = isFixedSize;
    }
  }
}
