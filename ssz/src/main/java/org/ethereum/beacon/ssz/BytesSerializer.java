package org.ethereum.beacon.ssz;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Serializer Class <-> bytes[] */
public interface BytesSerializer {
  /**
   * Serializes input
   *
   * @param input input value
   * @param clazz Class of value
   * @return serialization
   */
  <C> byte[] encode(@Nullable C input, Class<? extends C> clazz);

  /**
   * Shortcut to {@link #encode(Object, Class)}. Resolves class using input object. Not suitable for
   * null values.
   */
  default <C> byte[] encode(@Nonnull C input) {
    return encode(input, input.getClass());
  }

  /**
   * Restores data instance from serialization data and constructs instance of class with provided
   * data
   *
   * @param data Serialization data
   * @param clazz type class
   * @return deserialized instance of clazz or throws exception
   */
  <C> C decode(byte[] data, Class<? extends C> clazz);
}
