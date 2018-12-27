package org.ethereum.beacon.chain.storage;

import java.util.Optional;
import org.ethereum.beacon.core.Hashable;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface Hash32KeyStorage<T extends Hashable> {

  Optional<T> get(Hash32 hash);

  void put(Hash32 hash, T item);

  default void put(T item) {
    put(item.getHash(), item);
  }
}
