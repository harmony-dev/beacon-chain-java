package org.ethereum.beacon.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Cache made around LRU-map with fixed size, removing eldest entries (by added) when the space is
 * over
 *
 * @param <K> Keys type
 * @param <V> Values type
 */
public class LRUCache<K, V> implements Cache<K, V> {

  private final Map<K, V> cacheData;

  /**
   * Creates cache
   *
   * @param capacity Size of the cache
   */
  public LRUCache(int capacity) {
    this.cacheData =
        new LinkedHashMap<K, V>(capacity + 1, .75F, true) {
          // This method is called just after a new entry has been added
          public boolean removeEldestEntry(Map.Entry eldest) {
            return size() > capacity;
          }
        };
  }

  /**
   * Queries value from the cache. If it's not found there, fallback function is used to calculate
   * value. After calculation result is put in cache and returned.
   *
   * @param key Key to query
   * @param fallback Fallback function for calculation of the result in case of missed cache entry
   * @return expected value result for provided key
   */
  @Override
  public V get(K key, Function<K, V> fallback) {
    if (cacheData.containsKey(key)) {
      return cacheData.get(key);
    }

    V result = fallback.apply(key);
    cacheData.put(key, result);

    return result;
  }
}
