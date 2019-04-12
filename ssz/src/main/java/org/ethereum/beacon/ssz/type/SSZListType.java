package org.ethereum.beacon.ssz.type;


import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZListAccessor;

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

  public boolean isVector() {
    return vectorLength >= 0;
  }

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
  public int getChildrenCount(Object value) {
    return getAccessor().getChildrenCount(value);
  }

  @Override
  public Object getChild(Object value, int idx) {
    return getAccessor().getChildValue(value, idx);
  }

  @Override
  public SSZField getTypeDescriptor() {
    return descriptor;
  }
}
