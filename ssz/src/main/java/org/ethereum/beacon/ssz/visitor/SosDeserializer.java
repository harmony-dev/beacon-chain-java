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

import java.nio.ByteOrder;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
    VisitLater visitLater = null;
    int maxIndex = calcTypeMaxIndex(type);
    int firstOffset = Integer.MAX_VALUE;
    while (fixedPos < param.getValue0().size() && idx < maxIndex && fixedPos < firstOffset) {
      DecodeResult childRes =
          childVisitor.apply(idx, Pair.with(param.getValue0().slice(fixedPos), true));
      if (childRes.isFixedSize) {
        instanceBuilder.setChild(idx, childRes.decodedInstance);
      } else {
        isFixedSize = false;
        int offset = (Integer) childRes.decodedInstance;

        // First time we found some item with variable size
        if (firstOffset == Integer.MAX_VALUE) {
          firstOffset = offset;
        }

        final int idxBackup = idx;
        if (visitLater != null) {
          visitLater.run(offset);
        }
        visitLater =
            new VisitLater(
                param.getValue0().slice(offset),
                offset,
                objects -> {
                  DecodeResult res =
                      childVisitor.apply(
                          idxBackup,
                          Pair.with(objects.getValue0().slice(0, objects.getValue1()), false));
                  variablePartConsumed.addAndGet(res.readBytes);
                  instanceBuilder.setChild(idxBackup, res.decodedInstance);
                });
      }
      fixedPos += childRes.readBytes;
      idx++;
    }

    if (visitLater != null) {
      visitLater.run();
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

  private int deserializeLength(Bytes lenBytes) {
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

  class VisitLater {
    private final Bytes input;
    private final int inputOffset;
    private final Consumer<Pair<Bytes, Integer>> runLater;

    public VisitLater(Bytes input, int inputOffset, Consumer<Pair<Bytes, Integer>> runLater) {
      this.input = input;
      this.inputOffset = inputOffset;
      this.runLater = runLater;
    }

    public void run(int end) {
      runLater.accept(Pair.with(input, end - inputOffset));
    }

    public void run() {
      runLater.accept(Pair.with(input, input.size()));
    }
  }
}
