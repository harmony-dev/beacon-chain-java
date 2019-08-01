package org.ethereum.beacon.db.source.impl;

import java.util.function.Function;
import org.ethereum.beacon.db.source.CacheSizeEvaluator;

public class CacheSizeEvaluatorImpl<KeyType, ValueType>
    implements CacheSizeEvaluator<KeyType, ValueType> {

  private final Function<KeyType, Long> keyEvaluator;
  private final Function<ValueType, Long> valueEvaluator;

  private long size;

  public CacheSizeEvaluatorImpl(
      Function<KeyType, Long> keyEvaluator, Function<ValueType, Long> valueEvaluator) {
    this.keyEvaluator = keyEvaluator;
    this.valueEvaluator = valueEvaluator;
  }

  @Override
  public long getEvaluatedSize() {
    return size;
  }

  @Override
  public void reset() {
    size = 0;
  }

  @Override
  public void added(KeyType key, ValueType value) {
    size += keyEvaluator.apply(key);
    size += valueEvaluator.apply(value);
  }

  @Override
  public void removed(KeyType key, ValueType value) {
    size -= keyEvaluator.apply(key);
    size -= valueEvaluator.apply(value);
  }
}
