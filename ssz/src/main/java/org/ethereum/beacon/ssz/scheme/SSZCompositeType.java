package org.ethereum.beacon.ssz.scheme;

import org.ethereum.beacon.ssz.type.SSZCompositeAccessor;

public interface SSZCompositeType extends SSZType {

  int getChildrenCount(Object value);

  Object getChild(Object value, int idx);

  SSZCompositeAccessor getAccessor();
}
