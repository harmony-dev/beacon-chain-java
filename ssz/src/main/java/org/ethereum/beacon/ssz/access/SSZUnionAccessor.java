package org.ethereum.beacon.ssz.access;

import java.util.List;

/**
 * Handles containers (aka structures or ordered heterogenous collection of values),
 * is responsible of accessing its child values, their types and new instance creation
 */
public interface SSZUnionAccessor extends SSZCompositeAccessor {

  interface UnionAccessor extends CompositeAccessor {

    /**
     * Returns Container children type descriptors
     */
    List<SSZField> getChildDescriptors();

    int getTypeIndex(Object unionInstance);

    @Override
    default int getChildrenCount(Object compositeInstance) {
      return 1;
    }
  }

  UnionAccessor getAccessor(SSZField containerDescriptor);
}
