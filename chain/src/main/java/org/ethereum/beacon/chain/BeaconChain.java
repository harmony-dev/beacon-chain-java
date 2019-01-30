package org.ethereum.beacon.chain;

import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.core.BeaconState;
import org.reactivestreams.Publisher;

public interface BeaconChain {

  Publisher<BeaconTuple> getBlockStatesStream();

  Publisher<BeaconState> getSlotStatesStream();

  void init();
}
