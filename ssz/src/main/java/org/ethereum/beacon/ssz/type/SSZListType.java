package org.ethereum.beacon.ssz.type;


import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZListAccessor;
import org.ethereum.beacon.ssz.annotation.SSZ;

/**
 * Represent specific SSZ List or Vector type with child elements of specific type which defined
 * as 'ordered variable-length (for List) or fixed-length (for Vector)
 * homogenous collection of values ' by the
 * <a href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/simple-serialize.md#composite-types">
 *   SSZ spec</a>
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

  @Override
  public Type getType() {
    return getVectorLength() > VARIABLE_SIZE ? Type.VECTOR : Type.LIST;
  }

  /**
   * If this type represents SSZ Vector then this method returns its length.
   *
   * @see SSZ#vectorLength()
   * @see SSZ#vectorLengthVar()
   */
  public int getVectorLength() {
    return (int) accessor.fromAtomicSize(vectorLength);
  }

  @Override
  public int getSize() {
    if (getType() == Type.LIST || getElementType().isVariableSize()) {
      return VARIABLE_SIZE;
    }
    return getElementType().getSize() * getVectorLength();
  }

  public int getAtomicSize() {
    return vectorLength;
  }

  public long getMaxSize() {
    return accessor.fromAtomicSize(maxSize);
  }

  public long getMaxAtomicSize() {
    return maxSize;
  }

  /** Returns the {@link SSZType} of this list elements */
  @Override
  public SSZType getElementType() {
    if (elementType == null) {
      elementType = typeResolver.resolveSSZType(getAccessor().getListElementType(getTypeDescriptor()));
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
}
