package org.ethereum.beacon.pow;

import java.util.concurrent.CompletableFuture;

/** A dummy interface that could replaced with any more convenient pattern later. */
public interface PoWChainListener {

  /**
   * Waits for PoW chain's sync done event.
   *
   * @return a future that is completed when sync is done.
   */
  CompletableFuture<Void> getSyncDone();
}
