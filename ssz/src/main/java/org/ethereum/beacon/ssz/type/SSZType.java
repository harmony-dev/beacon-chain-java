package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.access.SSZField;

public interface SSZType {

  int VARIABLE_SIZE = -1;

  default boolean isBasicType() {
    return false;
  }

  default boolean isContainer() {
    return false;
  }

  default boolean isList() {
    return false;
  }

  default boolean isFixedSize() {
    return getSize() != VARIABLE_SIZE;
  }

  default boolean isVariableSize() {
    return !isFixedSize();
  }

  int getSize();

  SSZField getTypeDescriptor();

  default String toStringHelper() {
    String type;
    if (isBasicType()) {
      type = "Basic";
    } else if (isContainer()) {
      type = "Container";
    } else if (isList()) {
      if (isFixedSize()) {
        type = "Vector";
      } else {
        type = "List";
      }
    } else {
      type = "Unknown";
    }
    return "SSZType[" + type + ", size=" + getSize() + ", descr: " + getTypeDescriptor() + "]";
  }
}
