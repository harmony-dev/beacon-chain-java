package org.ethereum.beacon.db.source.impl;

import org.ethereum.beacon.db.source.DataSource;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Anton Nashatyrev on 19.11.2018.
 */
public class HashMapDataSource<K, V> implements DataSource<K, V> {

  Map<K, V> store = new ConcurrentHashMap<>();

  @Override
  public Optional<V> get(@Nonnull K key) {
    return Optional.ofNullable(store.get(key));
  }

  @Override
  public void put(@Nonnull K key, @Nonnull V value) {
    store.put(key, value);
  }

  @Override
  public void remove(@Nonnull K key) {
    store.remove(key);
  }

  @Override
  public void flush() {
    // nothing to do
  }

  public Map<K, V> getStore() {
    return store;
  }
}
