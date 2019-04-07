package org.ethereum.beacon.ssz.access;

import java.util.List;

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
