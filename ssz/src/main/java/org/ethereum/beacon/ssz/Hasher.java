package org.ethereum.beacon.ssz;

import java.util.List;

/**
 * Hasher
 *
 * @param <R> return type
 */
public interface Hasher<R> {
  /**
   * Calculatesh hash of object
   *
   * @param input Input object to calculate hash of
   * @return Hash
   */
  R calc(Object input);

  R calcList(List<Object> input);
}
