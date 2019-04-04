package org.ethereum.beacon.ssz.scheme;

import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

public interface SSZType {

  long VARIABLE_SIZE = -1;

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

  long getSize();

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
