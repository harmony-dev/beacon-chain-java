package org.ethereum.beacon.ssz.scheme;

import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

public interface TypeResolver {

  default SSZType resolveSSZType(Class<?> clazz) {
    return resolveSSZType(new SSZField(clazz));
  }

  SSZType resolveSSZType(SSZField descriptor);
}
