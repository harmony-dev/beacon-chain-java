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

  static DatabaseFlusher instant(WriteBuffer buffer) {
    return new InstantFlusher(buffer);
  }

  static DatabaseFlusher limitedToSize(WriteBuffer buffer, long bufferSizeLimit) {
    return new BufferSizeObserver(buffer, bufferSizeLimit);
  }

  enum Type {
    Instant,
    BufferSizeObserver
  }
}
