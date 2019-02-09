package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.db.source.DataSource;
import tech.pegasys.artemis.ethereum.core.Hash;

public interface HashKeyStorage<H extends Hash, T> extends DataSource<H, T> {

  /**
   * Puts an item into a storage using a hash of the item as its key.
   *
   * @param item an item.
   */
  void put(T item);
}
