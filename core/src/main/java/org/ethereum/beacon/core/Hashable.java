package org.ethereum.beacon.core;

import tech.pegasys.artemis.ethereum.core.Hash;

/** An interface of a hashable entity. */
public interface Hashable<H extends Hash> {

  /**
   * Returns a hash of the object.
   *
   * @return a hash.
   */
  H getHash();
}
