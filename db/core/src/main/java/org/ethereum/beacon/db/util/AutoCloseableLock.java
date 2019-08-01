package org.ethereum.beacon.db.util;

import java.util.concurrent.locks.Lock;

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
