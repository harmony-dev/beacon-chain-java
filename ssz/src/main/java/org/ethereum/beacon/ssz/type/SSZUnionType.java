package org.ethereum.beacon.ssz.type;

import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZUnionAccessor;
import tech.pegasys.artemis.util.collections.Union;

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
      List<SSZField> sszFields = accessor.getInstanceAccessor(getTypeDescriptor())
          .getChildDescriptors();
      if (sszFields.isEmpty()) {
        throw new SSZSchemeException("No Union members found: " + this.getTypeDescriptor());
      }
      for (int i = 1; i < sszFields.size(); i++) {
        if (sszFields.get(i).getRawClass() == Union.Null.class) {
          throw new SSZSchemeException("Union Null should be the only Null member at index 0");
        }
      }
      childTypes = sszFields.stream()
          .map(typeResolver::resolveSSZType)
          .collect(Collectors.toList());
    }
    return childTypes;
  }

  public boolean isNullable() {
    return getChildTypes().get(0).getTypeDescriptor().getRawClass() == Union.Null.class;
  }
}
