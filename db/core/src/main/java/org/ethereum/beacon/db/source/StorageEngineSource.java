package org.ethereum.beacon.db.source;

import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Data source supplier based on a specific key-value storage engine like RocksDB, LevelDB, etc.
 *
 * <p>Underlying implementation MUST support batch updates and MAY be aware of open and close
 * operations.
 *
 * @param <ValueType> a value type.
 */
public interface StorageEngineSource<ValueType>
    extends BatchUpdateDataSource<BytesValue, ValueType> {

  /**
   * Opens key-value storage.
   *
   * <p><strong>Note:</strong> an implementation MUST take care of double open calls.
   */
  void open();

  /** Closes key-value storage. */
  void close();
}
