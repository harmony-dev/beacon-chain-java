package org.ethereum.beacon.ssz;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Hasher Class -> bytes[] */
public interface BytesHasher {
  /**
   * Hashes input
   *
   * @param input Input value
   * @param clazz Class of value
   * @return serialization
   */
  <C> byte[] hash(@Nullable C input, Class<? extends C> clazz);

  /**
   * Hashes truncated input. Prepares virtual object, which gets all fields from input except
   * the last one.
   *
   * @param input Input value
   * @param clazz Class of value
   * @return serialization
   */
  <C> byte[] hashTruncateLast(@Nullable C input, Class<? extends C> clazz);

  /**
   * Shortcut to {@link #hash(Object, Class)}. Resolves class using input object. Not suitable for
   * null values.
   */
  default <C> byte[] hash(@Nonnull C input) {
    return hash(input, input.getClass());
  }
}
