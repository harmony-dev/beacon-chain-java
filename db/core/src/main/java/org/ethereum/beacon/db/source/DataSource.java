package org.ethereum.beacon.db.source;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Base class for data sources hierarchy
 * Declares general read/write methods for Key-Value storage
 *
 * The implementation could either:
 *   - propagate all updates immediately to the underlying storage
 *   - accumulate updates internally
 * In the latter case class should implement flush semantics
 */
public interface DataSource<KeyType, ValueType> extends ReadonlyDataSource<KeyType, ValueType> {

  /**
   * Returns the value corresponding to the key.
   * @param key Key in key-value Source
   * @return <code>Optional.empty()</code> if no entry exists
   */
  @Override
  Optional<ValueType> get(@Nonnull KeyType key);

  /**
   * Stores key-value entry.
   * If an entry with this key already exists, its value is overwritten
   * @param key Key
   * @param value Value
   */
  void put(@Nonnull KeyType key, @Nonnull ValueType value);

  /**
   * Removes key-value entry by its key.
   * If entry doesn't exist does nothing.
   * @param key Key
   */
  void remove(@Nonnull KeyType key);

  /**
   * If the implementation class accumulates any updates this method
   * should flush all the updates into underlying storage
   * If all updates are immediately propagated to the underlying
   * storage this method should do nothing.
   */
  void flush();
}
