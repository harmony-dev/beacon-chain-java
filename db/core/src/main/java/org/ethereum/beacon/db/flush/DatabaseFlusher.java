package org.ethereum.beacon.db.flush;

import org.ethereum.beacon.db.source.WriteBuffer;

/**
 * Flushing strategy.
 *
 * <p>Used to manage {@link WriteBuffer} flushes to underlying data source.
 *
 * @see InstantFlusher
 * @see BufferSizeObserver
 */
public interface DatabaseFlusher {

  /**
   * Forces a flush to the underlying data source.
   *
   * <p><strong>Note:</strong> an implementation MUST take care of consistency of the data being
   * force flushed.
   */
  void flush();

  /**
   * A client should call this method whenever a buffer contains consistent data that are ready to
   * be flushed.
   *
   * <p><strong>Note:</strong> depending on implementation this method MAY or MAY NOT be thread
   * safe.
   */
  void commit();
}
