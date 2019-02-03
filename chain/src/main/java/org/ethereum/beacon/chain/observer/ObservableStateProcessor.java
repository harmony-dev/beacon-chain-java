package org.ethereum.beacon.chain.observer;

import org.ethereum.beacon.chain.BeaconChainHead;
import org.reactivestreams.Publisher;

public interface ObservableStateProcessor {
  void start();

  Publisher<BeaconChainHead> getHeadStream();

  Publisher<ObservableBeaconState> getObservableStateStream();

  Publisher<PendingOperations> getPendingOperationsStream();
}
