package org.ethereum.beacon.db;

import org.ethereum.beacon.db.source.DataSource;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface Database {

  /**
   * Creates named key value storage if not yet exists or returns existing
   */
  DataSource<BytesValue, BytesValue> createStorage(String name);

  /**
   * Calling commit indicates that all current data is in consistent state
   * and it is a safe point to persist the data
   */
  void commit();

  /**
   * Close underlying database storage
   */
  void close();
}
