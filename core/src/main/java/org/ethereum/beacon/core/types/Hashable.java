package org.ethereum.beacon.core.types;

import tech.pegasys.artemis.ethereum.core.Hash;

import java.util.Optional;

/**
 * Indicates hashable type, with hash of type T
 *
 * @param <T> hash type, descendant of {@link Hash}
 */
public interface Hashable<T extends Hash> {
  /** Hash of the object */
  Optional<T> getHash();

  /** Sets hash for object, the only way to set it, but object itself could reset it at any time */
  void setHash(T hash);
}
