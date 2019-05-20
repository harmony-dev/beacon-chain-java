package org.ethereum.beacon.util.cache;

import java.util.Optional;
import java.util.function.Function;

/**
 * Checks validity of cache entry on access
 */
public class DebugCacheFactory implements CacheFactory {

  @Override
  public <K, V> Cache<K, V> createLRUCache(int capacity) {
    return new Cache<K, V>() {

      LRUCache<K, V> cache = new LRUCache<>(capacity);

      @Override
      public V get(K key, Function<K, V> fallback) {
        Optional<V> cacheEntry = cache.getExisting(key);
        if (cacheEntry.isPresent()) {
          V goldenVal = fallback.apply(key);
          if (!cacheEntry.get().equals(goldenVal)) {
            throw new IllegalStateException("Cache broken: key=" + key + ", cacheEntry: " + cacheEntry.get() + ", but should be: " + goldenVal);
          }
          return goldenVal;
        } else {
          return cache.get(key, fallback);
        }
      }
    };
  }
}
