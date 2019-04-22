package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.access.SSZField;

/**
 * Creates the SSZ type given the Java type descriptor
 */
public interface TypeResolver {

  /**
   * Resolves SSZ type from the Class representation.
   *
   * Note that raw class may not be sufficient to resolve the type.
   * If for example the {@link java.util.List} class is trying to be resolved it will
   * fail since element information from the parametrized type argument is not available.
   * For this case the {@link #resolveSSZType(SSZField)} variant should be used where
   * {@link SSZField#getParametrizedType()} would contain element type info
   *
   * Another case is where <code>int</code> value should be treated as SSZ <code>uint64</code> type
   * (Java <code>int</code> is treated as SSZ <code>uint32</code> by default).
   * In that case the {@link #resolveSSZType(SSZField)} variant should be used as well where
   * {@link SSZField#getExtraSize()} would contain concrete SSZ basic type
   */
  default SSZType resolveSSZType(Class<?> clazz) {
    return resolveSSZType(new SSZField(clazz));
  }

  /**
   * Resolves SSZ type by its Java type descriptor which contains extra type information
   * besides the <code>Class</code> to properly identify the correct SSZ type and appropriate
   * Java instance accessor
   */
  SSZType resolveSSZType(SSZField descriptor);
}
