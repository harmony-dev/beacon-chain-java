package org.ethereum.beacon.util.cache;

import java.util.function.Function;

/**
 * Cache
 *
 * @param <K> type of keys
 * @param <V> type of values
 */
public interface Cache<K, V> {
  /**
   * Queries value from the cache. If it's not found there, fallback function is used to calculate
   * value. After calculation result is put in cache and returned.
   *
   * @param key Key to query
   * @param fallback Fallback function for calculation of the result in case of missed cache entry
   * @return expected value result for provided key
   */
  V get(K key, Function<K, V> fallback);
}
