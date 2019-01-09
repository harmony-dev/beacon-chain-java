package org.ethereum.beacon.db.source.impl;

import org.ethereum.beacon.db.source.HoleyList;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HashMapHoleyList<V> implements HoleyList<V> {
  private final Map<Long, V> store = new HashMap<>();
  private long size = 0;

  @Override
  public long size() {
    return size;
  }

  @Override
  public void put(long idx, V value) {
    if (value == null) return;
    size = Math.max(size, idx + 1);
    store.put(idx, value);
  }

  @Override
  public Optional<V> get(long idx) {
    return Optional.ofNullable(store.get(idx));
  }
}
