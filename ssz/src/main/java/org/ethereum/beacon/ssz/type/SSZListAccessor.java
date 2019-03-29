package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

public interface SSZListAccessor {

  boolean isSupported(SSZField listDescriptor);

  default int getVectorSize(SSZField listDescriptor, Object listObject) {
    return -1;
  }

  default boolean isVector(SSZField listDescriptor) {
    return getVectorSize(listDescriptor, null) >= 0;
  }

  SSZField getListElementType(SSZField listDescriptor);

  long getChildCount(Object listObject);

  Object getChild(Object listObject, long index);
}
