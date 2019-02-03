package org.ethereum.beacon.chain.observer;

import org.reactivestreams.Publisher;

public interface PendingOperationsProcessor {
  Publisher<PendingOperations> getPendingOperationsStream();
}
