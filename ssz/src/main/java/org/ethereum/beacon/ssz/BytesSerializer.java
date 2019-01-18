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
  byte[] encode(@Nullable Object input, Class clazz);

  /**
   * Shortcut to {@link #encode(Object, Class)}. Resolves class using input object. Not suitable for
   * null values.
   */
  default byte[] encode(@Nonnull Object input) {
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
  Object decode(byte[] data, Class clazz);
}
