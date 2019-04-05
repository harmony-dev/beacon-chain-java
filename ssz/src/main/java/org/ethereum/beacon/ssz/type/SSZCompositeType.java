package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.access.SSZCompositeAccessor;

public interface SSZCompositeType extends SSZType {

  int getChildrenCount(Object value);

  Object getChild(Object value, int idx);

  SSZCompositeAccessor getAccessor();
}
