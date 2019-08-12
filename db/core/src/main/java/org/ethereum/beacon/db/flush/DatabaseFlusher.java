package org.ethereum.beacon.db.flush;

import org.ethereum.beacon.db.source.WriteBuffer;

public interface DatabaseFlusher {

  void flush();

  void commit();

  static DatabaseFlusher create(WriteBuffer buffer, long bufferSizeLimit) {
    return bufferSizeLimit > 0
        ? new BufferSizeObserver(buffer, bufferSizeLimit)
        : new InstantFlusher(buffer);
  }
}
