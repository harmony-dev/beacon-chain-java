package org.ethereum.beacon.chain;

import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.reactivestreams.Publisher;

public interface BeaconChain {

  Publisher<BeaconTuple> getBlockStream();

  void init();
}
