package org.ethereum.beacon.ssz.access;

import java.util.List;

/**
 * Handles containers (aka structures or ordered heterogenous collection of values),
 * is responsible of accessing its child values, their types and new instance creation
 */
public interface SSZContainerAccessor extends SSZCompositeAccessor {

  interface ContainerInstanceBuilder extends CompositeInstanceBuilder {

    /**
     * Sets the future instance child value by its descriptor
     */
    void setChild(SSZField childDescriptor, Object childValue);
  }

  interface ContainerAccessor extends CompositeAccessor {

    /**
     * Returns Container children type descriptors
     */
    List<SSZField> getChildDescriptors();

    @Override
    default int getChildrenCount(Object compositeInstance) {
      return getChildDescriptors().size();
    }
  }

  ContainerAccessor getAccessor(SSZField containerDescriptor);

  ContainerInstanceBuilder createInstanceBuilder(SSZField containerDescriptor);

}
