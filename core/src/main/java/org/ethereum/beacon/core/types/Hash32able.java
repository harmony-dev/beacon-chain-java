package org.ethereum.beacon.core.types;

import tech.pegasys.artemis.ethereum.core.Hash32;

import java.util.Optional;

/** Indicates type with {@link Hash32} hash */
public interface Hash32able {
  /** Hash of the object */
  Optional<Hash32> getHash();

  /** Sets hash for object, the only way to set it, but object itself could reset it at any time */
  void setHash(Hash32 hash);
}
