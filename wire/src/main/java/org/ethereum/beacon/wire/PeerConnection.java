package org.ethereum.beacon.wire;

import java.util.concurrent.CompletableFuture;

public interface PeerConnection {

  /**
   * Returns the future which completes when this connection is closed.
   */
  CompletableFuture<Void> getCloseFuture();

  /**
   * Closes this connection.
   */
  void close();
}
