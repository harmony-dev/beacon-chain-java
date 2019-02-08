package org.ethereum.beacon.consensus.hasher;

import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * An interface of object hasher.
 *
 * @param <H> a hash type.
 * @see SSZObjectHasher
 */
public interface ObjectHasher<H extends BytesValue> {

  /**
   * Calculates hash of given object.
   *
   * @param input an object of any type.
   * @return calculated hash.
   */
  H getHash(Object input);
}
