package org.ethereum.beacon.chain.observer;

import org.reactivestreams.Publisher;

public interface PendingStateProcessor {
  Publisher<PendingOperations> getPendingOperationsStream();
}
