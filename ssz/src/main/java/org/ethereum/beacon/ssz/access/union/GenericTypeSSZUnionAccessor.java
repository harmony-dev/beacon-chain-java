package org.ethereum.beacon.ssz.access.union;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZUnionAccessor;
import tech.pegasys.artemis.util.collections.GenericUnionImpl;
import tech.pegasys.artemis.util.collections.ReadUnion;
import tech.pegasys.artemis.util.collections.WriteUnion;

/**
 * Gathers information about Union member types from the generic types arguments
 * see {@link WriteUnion.U2}, {@link WriteUnion.U3}, etc.
 * Also see javadoc at {@link ReadUnion}
 */
public class GenericTypeSSZUnionAccessor implements SSZUnionAccessor {

  class GenericUnionAccessor implements UnionAccessor {
    private final List<SSZField> descriptors;

    public GenericUnionAccessor(SSZField unionDescriptor) {
      descriptors = getChildDescriptorsFromGenericType(unionDescriptor);
    }

    @Override
    public List<SSZField> getChildDescriptors() {
      return descriptors;
    }

    @Override
    public Object getChildValue(Object compositeInstance, int childIndex) {
      return ((ReadUnion) compositeInstance).getValue();
    }

    @Override
    public int getTypeIndex(Object unionInstance) {
      return ((ReadUnion) unionInstance).getTypeIndex();
    }
  }

  @Override
  public boolean isSupported(SSZField field) {
    return ReadUnion.GenericTypedUnion.class.isAssignableFrom(field.getRawClass());
  }

  @Override
  public CompositeInstanceBuilder createInstanceBuilder(SSZField unionDescriptor) {
    return new CompositeInstanceBuilder() {
      private WriteUnion union;

      @Override
      public void setChild(int idx, Object childValue) {
        union = new GenericUnionImpl();
        union.setValue(idx, childValue);
      }

      @Override
      public Object build() {
        return union;
      }
    };
  }

  @Override
  public UnionAccessor getAccessor(SSZField compositeDescriptor) {
    return new GenericUnionAccessor(compositeDescriptor);
  }

  private List<SSZField> getChildDescriptorsFromGenericType(SSZField unionDescriptor) {
    if (!ReadUnion.GenericTypedUnion.class.isAssignableFrom(unionDescriptor.getRawClass())) {
      throw new IllegalArgumentException("Unknown union class: " + unionDescriptor);
    }
    Type[] typeArguments = unionDescriptor.getParametrizedType().getActualTypeArguments();
    return Arrays.stream(typeArguments).map(SSZField::new).collect(Collectors.toList());
  }
}
