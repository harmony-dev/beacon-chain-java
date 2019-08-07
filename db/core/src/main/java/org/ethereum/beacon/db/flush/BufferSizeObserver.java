package org.ethereum.beacon.db.flush;

import org.ethereum.beacon.db.source.WriteBuffer;

public class BufferSizeObserver implements DatabaseFlusher {

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
      flush();
    }
  }
}
