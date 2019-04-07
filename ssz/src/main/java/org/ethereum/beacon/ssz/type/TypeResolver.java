package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.access.SSZField;

public interface TypeResolver {

  default SSZType resolveSSZType(Class<?> clazz) {
    return resolveSSZType(new SSZField(clazz));
  }

  SSZType resolveSSZType(SSZField descriptor);
}
