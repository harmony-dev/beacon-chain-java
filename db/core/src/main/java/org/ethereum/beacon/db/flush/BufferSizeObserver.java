package org.ethereum.beacon.db.flush;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.db.source.WriteBuffer;

public class BufferSizeObserver implements DatabaseFlusher {

  private static final Logger logger = LogManager.getLogger(BufferSizeObserver.class);

  private final WriteBuffer buffer;
  private final WriteBuffer commitTrack;
  private final long bufferSizeLimit;

  BufferSizeObserver(WriteBuffer buffer, WriteBuffer commitTrack, long bufferSizeLimit) {
    this.buffer = buffer;
    this.commitTrack = commitTrack;
    this.bufferSizeLimit = bufferSizeLimit;
  }

  public static <K, V> BufferSizeObserver create(WriteBuffer<K, V> buffer, long bufferSizeLimit) {
    WriteBuffer<K, V> commitTrack = new WriteBuffer<>(buffer, false);
    return new BufferSizeObserver(buffer, commitTrack, bufferSizeLimit);
  }

  @Override
  public void flush() {
    buffer.flush();
  }

  @Override
  public void commit() {
    commitTrack.flush();
    if (buffer.evaluateSize() >= bufferSizeLimit) {
      logger.trace(
          "Flush db buffer due to {}M >= {}M",
          buffer.evaluateSize() >>> 20,
          bufferSizeLimit >>> 20);
      flush();
    }
  }
}
