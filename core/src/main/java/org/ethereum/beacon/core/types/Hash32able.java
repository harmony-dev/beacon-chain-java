package org.ethereum.beacon.core.types;

import tech.pegasys.artemis.ethereum.core.Hash32;

import java.util.Optional;
import java.util.function.Function;

/** Indicates type with {@link Hash32} hash */
public interface Hash32able {
  ObjectHasherHolder objectHasher = new ObjectHasherHolder();

  /**
   * Hash of the object calculated with {@link #objectHasher}
   *
   * @return hash of this object
   */
  Optional<Hash32> getHash();

  class ObjectHasherHolder {
    private Function<Object, Hash32> objectHasher = null;

    public Optional<Function<Object, Hash32>> getObjectHasher() {
      return Optional.ofNullable(objectHasher);
    }

    /**
     * Sets object hasher for all objects of {@link Hash32able} type
     * @param objectHasher
     */
    public void setObjectHasher(Function<Object, Hash32> objectHasher) {
      this.objectHasher = objectHasher;
    }
  }
}
