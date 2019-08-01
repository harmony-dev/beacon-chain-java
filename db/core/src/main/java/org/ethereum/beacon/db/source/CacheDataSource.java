package org.ethereum.beacon.db.source;

import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Represents {@link DataSource} which caches upstream key-value entries (either on read, write or both)
 */
public interface CacheDataSource<KeyType, ValueType>
    extends LinkedDataSource<KeyType, ValueType, KeyType, ValueType> {

  /**
   * Returns the entry if it's currently in the cache.
   * Shouldn't query upsource for data if entry not found in the cache.
   *
   * If the value is not cached returns
   *   <code>Optional.empty()</code>
   * If the value cached and the value is null returns
   *   <code>Optional.of(Optional.empty())</code>
   * If the value cached and the value is not null returns
   *   <code>Optional.of(Optional.of(value))</code>
   */
  Optional<Optional<ValueType>> getCacheEntry(@Nonnull KeyType key);

  /**
   * Evaluates a number of bytes occupied by cached objects in memory.
   *
   * @return size in memory.
   */
  long evaluateSize();
}
