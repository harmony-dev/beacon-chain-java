package org.ethereum.beacon.db;

public interface Database {

  void flushSync();

  void commit();
}
