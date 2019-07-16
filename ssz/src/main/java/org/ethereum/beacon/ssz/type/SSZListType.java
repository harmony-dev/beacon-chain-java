package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.SSZDeserializeException;
import org.ethereum.beacon.ssz.SSZSerializeException;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZListAccessor;
import org.ethereum.beacon.ssz.annotation.SSZ;

/**
 * Represent specific SSZ List or Vector type with child elements of specific type which defined as
 * 'ordered variable-length (for List) or fixed-length (for Vector) homogenous collection of values
 * ' by the <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/simple-serialize.md#composite-types">
 * SSZ spec</a>
 */
public class SSZListType implements SSZHomoCompositeType {

  private final SSZField descriptor;
  private final TypeResolver typeResolver;
  private final SSZListAccessor accessor;
  private final int vectorLength;
  private final long maxSize;

  private SSZType elementType;

  public SSZListType(
      SSZField descriptor,
      TypeResolver typeResolver,
      SSZListAccessor accessor,
      int vectorLength,
      long maxSize) {
    if (vectorLength > VARIABLE_SIZE && maxSize > VARIABLE_SIZE) {
      throw new RuntimeException("Only vectorLength or maxSize should be set at time");
    }
    this.descriptor = descriptor;
    this.typeResolver = typeResolver;
    this.accessor = accessor;
    this.vectorLength = vectorLength;
    this.maxSize = maxSize;
  }

  public void verifyMovingLimit(int childrenCount) {
    if (getType() == Type.VECTOR && !isBitType()) {
      if (childrenCount > getVectorLength()) {
        throw new SSZDeserializeException(
            String.format(
                "Vector type length exceeds actual list length: %d !=  %d for %s",
                getVectorLength(), childrenCount, toStringHelper()));
      }
    } else if (getType() == Type.LIST && getMaxSize() > VARIABLE_SIZE && !isBitType()) {
      if (childrenCount > getMaxSize()) {
        throw new SSZDeserializeException(
            String.format(
                "Maximum size of list is exceeded with actual number of elements: %d > %d for %s",
                childrenCount, getMaxSize(), toStringHelper()));
      }
    }
  }

  public void verifyLimit(Object param) {
    if (getType() == Type.VECTOR && !isBitType()) {
      if (getChildrenCount(param) != getVectorLength()) {
        throw new SSZSerializeException(
            String.format(
                "Vector type length doesn't match actual list length: %d !=  %d for %s",
                getVectorLength(), getChildrenCount(param), toStringHelper()));
      }
    } else if (getType() == Type.LIST && getMaxSize() > VARIABLE_SIZE && !isBitType()) {
      if (getChildrenCount(param) > getMaxSize()) {
        throw new SSZSerializeException(
            String.format(
                "Maximum size of list is exceeded with actual number of elements: %d > %d for %s",
                getChildrenCount(param), getMaxSize(), toStringHelper()));
      }
    }
  }

  @Override
  public Type getType() {
    return vectorLength > VARIABLE_SIZE ? Type.VECTOR : Type.LIST;
  }

  /**
   * If this type represents SSZ Vector then this method returns its length.
   *
   * @see SSZ#vectorLength()
   * @see SSZ#vectorLengthVar()
   */
  public int getVectorLength() {
    return vectorLength;
  }

  @Override
  public int getSize() {
    if (getType() == Type.LIST || getElementType().isVariableSize()) {
      return VARIABLE_SIZE;
    }
    return getElementType().getSize() * getVectorLength();
  }

  public long getMaxSize() {
    return maxSize;
  }

  /** Returns the {@link SSZType} of this list elements */
  @Override
  public SSZType getElementType() {
    if (elementType == null) {
      elementType =
          typeResolver.resolveSSZType(getAccessor().getListElementType(getTypeDescriptor()));
    }
    return elementType;
  }

  public SSZListAccessor getAccessor() {
    return accessor;
  }

  @Override
  public SSZField getTypeDescriptor() {
    return descriptor;
  }

  /** Indicates list bit type */
  public boolean isBitType() {
    return false;
  }
}
