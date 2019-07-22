package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.access.SSZField;

import java.util.Optional;

/** Resolves any kind of parameter for {@link SSZType} */
public interface TypeParameterResolver<T> {
  Optional<T> resolveTypeParameter(SSZField descriptor);
}
