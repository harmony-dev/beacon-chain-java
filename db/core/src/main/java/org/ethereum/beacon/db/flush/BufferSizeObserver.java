package org.ethereum.beacon.db.flush;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.db.source.WriteBuffer;

public class BufferSizeObserver implements DatabaseFlusher {

  private final static Logger logger = LogManager.getLogger(BufferSizeObserver.class);

  private final WriteBuffer buffer;
  private final long bufferSizeLimit;

  public BufferSizeObserver(WriteBuffer buffer, long bufferSizeLimit) {
    this.buffer = buffer;
    this.bufferSizeLimit = bufferSizeLimit;
  }

  @Override
  public void flush() {
    buffer.flush();
  }

  @Override
  public void commit() {
    if (buffer.evaluateSize() >= bufferSizeLimit) {
      logger.trace(
          "Flush db buffer due to {}M >= {}M",
          buffer.evaluateSize() >>> 20,
          bufferSizeLimit >>> 20);
      flush();
    }
  }
}
