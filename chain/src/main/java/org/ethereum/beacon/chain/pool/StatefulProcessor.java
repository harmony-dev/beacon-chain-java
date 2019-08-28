package org.ethereum.beacon.chain.pool;

/**
 * Stateful processor.
 *
 * <p>A processor that requires particular inner state to be initialized before it could be safely
 * called by its clients.
 *
 * <p>{@link #isInitialized()} method indicates whether processor has already been initialized or
 * not. It's a client responsibility to check {@link #isInitialized()} result before calling to the
 * instance of this processor.
 *
 * <p>Implementor MAY throw an {@link AssertionError} if it's been called before inner state has
 * been initialised.
 */
public interface StatefulProcessor {

  /**
   * Checks whether processor state is initialized or not.
   *
   * @return {@code true} if processor is ready to work, {@link false}, otherwise.
   */
  boolean isInitialized();
}
