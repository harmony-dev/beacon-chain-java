package org.ethereum.beacon.db.source;

import java.util.function.Function;
import org.ethereum.beacon.db.source.impl.CacheSizeEvaluatorImpl;

public interface CacheSizeEvaluator<KeyType, ValueType> {

  long getEvaluatedSize();

  void reset();

  void added(KeyType key, ValueType value);

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
