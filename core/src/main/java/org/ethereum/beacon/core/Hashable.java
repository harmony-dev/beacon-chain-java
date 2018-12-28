package org.ethereum.beacon.core;

import tech.pegasys.artemis.ethereum.core.Hash32;

/** An interface of a hashable entity. */
public interface Hashable {

  /**
   * Returns a hash of the object.
   *
   * @return a hash.
   */
  Hash32 getHash();
}
