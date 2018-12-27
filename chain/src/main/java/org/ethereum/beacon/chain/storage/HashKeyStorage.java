package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.core.Hashable;
import org.ethereum.beacon.db.source.DataSource;
import tech.pegasys.artemis.ethereum.core.Hash;

public interface HashKeyStorage<H extends Hash, T extends Hashable<H>>
    extends DataSource<H, T> {

  default void put(T item) {
    put(item.getHash(), item);
  }

  default void remove(T item) {
    remove(item.getHash());
  }
}
