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
public class SSZListType implements SSZCompositeType {

  private final SSZField descriptor;
  private final TypeResolver typeResolver;
  private final SSZListAccessor accessor;
  private final int vectorLength;

  private SSZType elementType;

  public SSZListType(SSZField descriptor, TypeResolver typeResolver,
      SSZListAccessor accessor, int vectorLength) {
    this.descriptor = descriptor;
    this.typeResolver = typeResolver;
    this.accessor = accessor;
    this.vectorLength = vectorLength;
  }

  @Override
  public boolean isList() {
    return true;
  }

  /**
   * Indicates if this type represents fixed-length SSZ Vector.
   * if <code>false</code> this type represents variable-length SSZ List
   */
  public boolean isVector() {
    return vectorLength >= 0;
  }

  /**
   * If this type represents SSZ Vector then this method returns its length.
   * @see SSZ#vectorSize()
   * @see SSZ#vectorSizeVar()
   */
  public int getVectorLength() {
    return vectorLength;
  }

  @Override
  public int getSize() {
    if (!isVector() || getElementType().isVariableSize()) {
      return VARIABLE_SIZE;
    }
    return getElementType().getSize() * vectorLength;
  }

  /**
   * Returns the {@link SSZType} of this list elements
   */
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
