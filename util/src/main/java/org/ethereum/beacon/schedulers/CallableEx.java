package org.ethereum.beacon.schedulers;

/**
 * The same as standard <code>Callable</code> which can throw unchecked exception
 */
public interface CallableEx<T> {
  T call() throws Throwable;
}
