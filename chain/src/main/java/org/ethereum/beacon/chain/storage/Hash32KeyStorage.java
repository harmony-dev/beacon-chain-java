package org.ethereum.beacon.chain.storage;

import java.util.Optional;
import org.ethereum.beacon.core.Hashable;
import org.ethereum.beacon.db.source.DataSource;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface Hash32KeyStorage<T extends Hashable>
    extends DataSource<Hash32, T> {

  default void put(T item) {
    put(item.getHash(), item);
  }

  default void remove(T item) {
    remove(item.getHash());
  }
}
