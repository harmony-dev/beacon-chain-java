package org.ethereum.beacon.ssz.access;

import java.util.List;

/**
 * Handles containers (aka structures or ordered heterogenous collection of values),
 * is responsible of accessing its child values, their types and new instance creation
 */
public interface SSZContainerAccessor extends SSZCompositeAccessor {

  interface ContainerInstanceAccessor extends CompositeInstanceAccessor {

    /**
     * Returns Container children type descriptors
     */
    List<SSZField> getChildDescriptors();

    @Override
    default int getChildrenCount(Object compositeInstance) {
      return getChildDescriptors().size();
    }
  }

  ContainerInstanceAccessor getInstanceAccessor(SSZField containerDescriptor);
}
