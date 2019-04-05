package org.ethereum.beacon.ssz.access;

import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

public interface SSZListAccessor extends SSZCompositeAccessor, SSZCompositeAccessor.CompositeAccessor{

  interface ListInstanceBuilder extends CompositeInstanceBuilder {

    void addChild(Object childValue);
  }

  @Override
  int getChildrenCount(Object value);

  @Override
  Object getChildValue(Object value, int idx);

  SSZField getListElementType(SSZField field);

  @Override
  ListInstanceBuilder createInstanceBuilder(SSZField listType);

  @Override
  default CompositeAccessor getAccessor(SSZField compositeDescriptor) {
    return this;
  }
}
