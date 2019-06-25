package org.ethereum.beacon.ssz.type;

import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.ssz.access.SSZContainerAccessor;
import org.ethereum.beacon.ssz.access.SSZField;

/**
 * Represent specific SSZ Container type with specific members which defined
 * as 'ordered heterogenous collection of values' by the
 * <a href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/simple-serialize.md#composite-types">
 *   SSZ spec</a>
 */
public class SSZContainerType implements SSZHeteroCompositeType {

  private final TypeResolver typeResolver;
  private final SSZField descriptor;
  private final SSZContainerAccessor containerAccessor;
  private final SSZContainerAccessor.ContainerAccessor accessor;

  private List<SSZType> childTypes;

  protected SSZContainerType() {
    this.typeResolver = null;
    this.descriptor = null;
    this.containerAccessor = null;
    this.accessor = null;
  }

  public SSZContainerType(TypeResolver typeResolver,
      SSZField descriptor, SSZContainerAccessor accessor) {
    this.typeResolver = typeResolver;
    this.descriptor = descriptor;
    this.containerAccessor = accessor;
    this.accessor = accessor.getAccessor(descriptor);
  }

  @Override
  public Type getType() {
    return Type.CONTAINER;
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

  @Override
  public List<SSZType> getChildTypes() {
    if (childTypes == null) {
      childTypes = accessor.getChildDescriptors().stream()
          .map(typeResolver::resolveSSZType)
          .collect(Collectors.toList());
    }
    return childTypes;
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
