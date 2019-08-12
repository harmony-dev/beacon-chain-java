package org.ethereum.beacon.db.flush;

import org.ethereum.beacon.db.source.WriteBuffer;

public class InstantFlusher implements DatabaseFlusher {

  private final WriteBuffer buffer;

  public InstantFlusher(WriteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public void flush() {}

  @Override
  public void commit() {
    buffer.flush();
  }
}
