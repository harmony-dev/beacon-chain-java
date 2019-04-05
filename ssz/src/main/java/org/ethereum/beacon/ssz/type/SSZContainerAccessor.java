package org.ethereum.beacon.ssz.type;

import java.util.List;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.type.SSZCompositeAccessor.CompositeAccessor;

public interface SSZContainerAccessor extends SSZCompositeAccessor {

  interface ContainerInstanceBuilder extends CompositeInstanceBuilder {

    void setChild(SSZField childDescriptor, Object childValue);
  }

  interface ContainerAccessor extends CompositeAccessor {

    List<SSZField> getChildDescriptors();

    @Override
    default int getChildrenCount(Object compositeInstance) {
      return getChildDescriptors().size();
    }
  }

  ContainerAccessor getAccessor(SSZField containerDescriptor);

  ContainerInstanceBuilder createInstanceBuilder(SSZField containerDescriptor);

}
