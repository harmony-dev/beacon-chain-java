package org.ethereum.beacon.ssz.access;

import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

public interface SSZCompositeAccessor {

  interface CompositeInstanceBuilder {

    void setChild(int idx, Object childValue);

    Object build();
  }

  interface CompositeAccessor {

    Object getChildValue(Object compositeInstance, int childIndex);

    int getChildrenCount(Object compositeInstance);
  }

  boolean isSupported(SSZField field);

  CompositeInstanceBuilder createInstanceBuilder(SSZField compositeDescriptor);

  CompositeAccessor getAccessor(SSZField compositeDescriptor);
}
