package org.ethereum.beacon.db.util;

import java.util.concurrent.locks.Lock;

/**
 * Releases lock after exit from try block.
 *
 * <p>Usage:
 *
 * <pre>
 *    try (AutoCloseableLock l = lock.lock()) {
 *      ...
 *    }
 * </pre>
 */
public final class AutoCloseableLock implements AutoCloseable {

  private final Lock delegate;

  AutoCloseableLock(Lock delegate) {
    this.delegate = delegate;
  }

  public static AutoCloseableLock wrap(Lock delegate) {
    return new AutoCloseableLock(delegate);
  }

  @Override
  public void close() {
    this.unlock();
  }

  public AutoCloseableLock lock() {
    delegate.lock();
    return this;
  }

  public void unlock() {
    delegate.unlock();
  }
}
