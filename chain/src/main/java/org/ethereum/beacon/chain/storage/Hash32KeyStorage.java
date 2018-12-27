package org.ethereum.beacon.chain.storage;

import tech.pegasys.artemis.ethereum.core.Hash32;

public interface Hash32KeyStorage<T> {

  T get(Hash32 hash);

  void put(Hash32 hash, T item);
}
