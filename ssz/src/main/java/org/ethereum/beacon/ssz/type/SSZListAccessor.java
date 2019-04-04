package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.scheme.SSZListType;

public interface SSZListAccessor {

  interface InstanceBuilder {

    void addChild(Object childValue);

    void setChild(int idx, Object childValue);

    Object build();
  }

  boolean isSupported(SSZField field);

  int getChildrenCount(Object value);

  Object getChild(Object value, int idx);

  SSZField getListElementType(SSZField field);

  InstanceBuilder createInstanceBuilder(SSZListType listType);
}
