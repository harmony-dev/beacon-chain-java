package org.ethereum.beacon.ssz.scheme;

import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.type.SSZContainerAccessor;

public class SSZContainerType implements SSZCompositeType {

  private final TypeResolver typeResolver;
  private final SSZField descriptor;
  private final SSZContainerAccessor containerAccessor;
  private final SSZContainerAccessor.ContainerAccessor accessor;

  public SSZContainerType(TypeResolver typeResolver,
      SSZField descriptor, SSZContainerAccessor accessor) {
    this.typeResolver = typeResolver;
    this.descriptor = descriptor;
    this.containerAccessor = accessor;
    this.accessor = accessor.getAccessor(descriptor);
  }

  @Override
  public boolean isContainer() {
    return true;
  }

  @Override
  public int getSize() {
    int size = 0;
    for (SSZType child : getChildTypes()) {
      long childSize = child.getSize();
      if (childSize < 0) {
        return VARIABLE_SIZE;
      }
      size += childSize;
    }
    return size;
  }

  public List<SSZType> getChildTypes() {
    return accessor.getChildDescriptors().stream()
        .map(typeResolver::resolveSSZType)
        .collect(Collectors.toList());
  }

  public List<String> getChildNames() {
    return accessor.getChildDescriptors().stream()
        .map(d -> d.name)
        .collect(Collectors.toList());
  }

  public SSZContainerAccessor getAccessor() {
    return containerAccessor;
  }

  @Override
  public int getChildrenCount(Object value) {
    return accessor.getChildDescriptors().size();
  }

  @Override
  public Object getChild(Object value, int idx) {
    return accessor.getChildValue(value, idx);
  }

  @Override
  public SSZField getTypeDescriptor() {
    return descriptor;
  }
}
