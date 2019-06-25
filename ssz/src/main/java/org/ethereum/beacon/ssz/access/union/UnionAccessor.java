package org.ethereum.beacon.ssz.access.union;

import java.util.List;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZUnionAccessor;
import org.ethereum.beacon.ssz.access.container.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.access.container.SSZSchemeBuilder.SSZScheme;
import tech.pegasys.artemis.util.collections.ReadUnion;
import tech.pegasys.artemis.util.collections.WriteUnion;

public class UnionAccessor implements SSZUnionAccessor {

  class SchemeUnionAccessor implements UnionAccessor {
    private final SSZScheme scheme;

    public SchemeUnionAccessor(SSZField containerDescriptor) {
      scheme = sszSchemeBuilder.build(containerDescriptor.getRawClass());
    }

    @Override
    public List<SSZField> getChildDescriptors() {
      return scheme.getFields();
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

  private final SSZSchemeBuilder sszSchemeBuilder;

  public UnionAccessor(SSZSchemeBuilder sszSchemeBuilder) {
    this.sszSchemeBuilder = sszSchemeBuilder;
  }

  @Override
  public boolean isSupported(SSZField field) {
    return ReadUnion.class.isAssignableFrom(field.getRawClass());
  }

  @Override
  public CompositeInstanceBuilder createInstanceBuilder(SSZField compositeDescriptor) {
    return new CompositeInstanceBuilder() {
      private WriteUnion union;

      @Override
      public void setChild(int idx, Object childValue) {
        try {
          union = (WriteUnion) compositeDescriptor.getRawClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public Object build() {
        return union;
      }
    };
  }

  @Override
  public UnionAccessor getAccessor(SSZField compositeDescriptor) {
    return new SchemeUnionAccessor(compositeDescriptor);
  }
}
