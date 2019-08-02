package org.ethereum.beacon.util.cache;

public interface CacheFactory {

  <K, V> Cache<K, V> createLRUCache(int capacity);

  static CacheFactory create(boolean enabled) {
    if (enabled) {
      return new RegularCacheFactory();
    } else {
      return new NoCacheFactory();
    }
  }
}
