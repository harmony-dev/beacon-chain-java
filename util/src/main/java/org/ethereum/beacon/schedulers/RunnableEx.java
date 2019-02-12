package org.ethereum.beacon.schedulers;

/**
 * The same as standard <code>Runnable</code> which can throw unchecked exception
 */
public interface RunnableEx {
  void run() throws Throwable;
}
