package org.ethereum.beacon.db.flush;

import org.ethereum.beacon.db.source.WriteBuffer;

public interface DatabaseFlusher {

  void flush();

  void commit();

  static DatabaseFlusher instant(WriteBuffer buffer) {
    return new InstantFlusher(buffer);
  }

  static DatabaseFlusher limitedToSize(WriteBuffer buffer, long bufferSizeLimit) {
    return new BufferLimitFlusher(buffer, bufferSizeLimit);
  }
}
