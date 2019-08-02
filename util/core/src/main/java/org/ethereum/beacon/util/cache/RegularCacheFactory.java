package org.ethereum.beacon.util.cache;

public class RegularCacheFactory implements CacheFactory {

  @Override
  public <K, V> Cache<K, V> createLRUCache(int capacity) {
    return new LRUCache<K, V>(capacity);
  }
}
