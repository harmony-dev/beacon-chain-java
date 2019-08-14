package org.ethereum.beacon.db.source;

import java.util.function.Function;
import org.ethereum.beacon.db.source.impl.CacheSizeEvaluatorImpl;

/**
 * Evaluates a number of bytes occupied by cache or buffer in memory.
 *
 * @param <KeyType> a key type.
 * @param <ValueType> a value type.
 * @see WriteBuffer
 */
public interface CacheSizeEvaluator<KeyType, ValueType> {

  /**
   * Returns cache size in bytes.
   *
   * @return a number of bytes.
   */
  long getEvaluatedSize();

  /** This method MUST be called whenever cache gets reset. */
  void reset();

  /**
   * This method MUST be called whenever new entry is added to the cache.
   *
   * @param key a key.
   * @param value a value.
   */
  void added(KeyType key, ValueType value);

  /**
   * This method MUST be called whenever new entry is removed from the cache.
   *
   * @param key a key.
   * @param value a value.
   */
  void removed(KeyType key, ValueType value);

  static <KeyType, ValueType> CacheSizeEvaluator<KeyType, ValueType> noSizeEvaluator() {
    return getInstance((KeyType key) -> 0L, (ValueType key) -> 0L);
  }

  static <KeyValueType> CacheSizeEvaluator<KeyValueType, KeyValueType> getInstance(
      Function<KeyValueType, Long> keyValueEvaluator) {
    return getInstance(keyValueEvaluator, keyValueEvaluator);
  }

  static <KeyType, ValueType> CacheSizeEvaluator<KeyType, ValueType> getInstance(
      Function<KeyType, Long> keyEvaluator, Function<ValueType, Long> valueEvaluator) {
    return new CacheSizeEvaluatorImpl<>(keyEvaluator, valueEvaluator);
  }
}
