package org.ethereum.beacon.db.source;

import java.util.Map;

/**
 * {@link DataSource} which supports batch updates.
 * Batch update can be either atomic or not depending on the implementation
 */
public interface BatchUpdateDataSource<KeyType, ValueType> extends DataSource<KeyType, ValueType> {

  /**
   * Applies passed updates to this source.
   * If the implementing class supports atomic updates, then the changes passed
   * shouldn't be visible partially to a code querying this source from another thread
   * @param updates the Map should be treated as unmodifiable Collection of Key-Value pairs with unique Keys.
   *                A pair with non-null value represents <code>put</code> operation
   *                A pair with <code>null</code> value represents <code>remove</code> operation
   */
  void batchUpdate(Map<KeyType, ValueType> updates);
}
