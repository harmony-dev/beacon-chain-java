package org.ethereum.beacon.db.source;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Represents read-only {@link DataSource}
 */
public interface ReadonlyDataSource<KeyType, ValueType> {

  /**
   * Returns the value corresponding to the key.
   * @param key Key in key-value Source
   * @return <code>Optional.empty()</code> if no entry exists
   */
  Optional<ValueType> get(@Nonnull KeyType key);
}
