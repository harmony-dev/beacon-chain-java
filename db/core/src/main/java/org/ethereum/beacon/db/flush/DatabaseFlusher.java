package org.ethereum.beacon.db.flush;

public interface DatabaseFlusher {

  void flush();

  void commit();
}
