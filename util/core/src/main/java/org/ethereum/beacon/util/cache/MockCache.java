package org.ethereum.beacon.util.cache;

import java.util.function.Function;
import org.ethereum.beacon.util.cache.Cache;

/**
 * Cache without cache proxying all requests to fallback function
 *
 * @param <K> Keys type
 * @param <V> Values type
 */
public class MockCache<K, V> implements Cache<K, V> {
  /** Creates cache */
  public MockCache() {}

  /**
   * Just calls fallback to calculate result and returns it as it's mock cache
   *
   * @param key Key to query
   * @param fallback Fallback function for calculation of the result
   * @return expected value result for provided key
   */
  @Override
  public V get(K key, Function<K, V> fallback) {
    return fallback.apply(key);
  }
}
