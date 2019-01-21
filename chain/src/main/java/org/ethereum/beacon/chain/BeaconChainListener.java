package org.ethereum.beacon.chain;

import java.util.concurrent.CompletableFuture;

/** A stub for beacon chain listener. */
public interface BeaconChainListener {
  CompletableFuture<Void> getSyncDone();
}
