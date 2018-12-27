package org.ethereum.beacon.db;

public interface DBFlusher {

  void flushSync();

  void commit();
}
