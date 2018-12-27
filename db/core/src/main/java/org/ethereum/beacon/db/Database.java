package org.ethereum.beacon.db;

public interface Database {

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
