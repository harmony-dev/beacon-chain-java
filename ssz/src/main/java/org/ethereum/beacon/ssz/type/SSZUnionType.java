package org.ethereum.beacon.ssz.type;

import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZUnionAccessor;
import tech.pegasys.artemis.util.collections.ReadUnion;

public class SSZUnionType implements SSZHeteroCompositeType {

  private final SSZUnionAccessor accessor;
  private final SSZField descriptor;
  private final TypeResolver typeResolver;

  private List<SSZType> childTypes;

  public SSZUnionType(SSZUnionAccessor accessor, SSZField descriptor,
      TypeResolver typeResolver) {
    this.accessor = accessor;
    this.descriptor = descriptor;
    this.typeResolver = typeResolver;
  }

  @Override
  public Type getType() {
    return Type.UNION;
  }

  @Override
  public int getSize() {
    return VARIABLE_SIZE;
  }

  @Override
  public SSZUnionAccessor getAccessor() {
    return accessor;
  }

  @Override
  public SSZField getTypeDescriptor() {
    return descriptor;
  }

  @Override
  public List<SSZType> getChildTypes() {
    if (childTypes == null) {
      childTypes = accessor.getAccessor(getTypeDescriptor()).getChildDescriptors()
          .stream()
          .map(typeResolver::resolveSSZType)
          .collect(Collectors.toList());
    }
    return childTypes;
  }

  public boolean isNullable() {
    return getChildTypes().get(0).getTypeDescriptor().getRawClass() == ReadUnion.Null.class;
  }
}
