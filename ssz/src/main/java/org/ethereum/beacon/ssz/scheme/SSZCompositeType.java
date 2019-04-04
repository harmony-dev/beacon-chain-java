package org.ethereum.beacon.ssz.scheme;

public interface SSZCompositeType extends SSZType {

  int getChildrenCount(Object value);

  Object getChild(Object value, int idx);
}
