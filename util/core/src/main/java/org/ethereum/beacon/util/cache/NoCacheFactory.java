package org.ethereum.beacon.util.cache;

public class NoCacheFactory implements CacheFactory {

  @Override
  public <K, V> Cache<K, V> createLRUCache(int capacity) {
    return new MockCache<K, V>();
  }
}
