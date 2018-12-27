package org.ethereum.beacon.chain;

import org.ethereum.beacon.core.BeaconBlock;

public interface MutableBeaconChain extends BeaconChain {

  void insert(BeaconBlock block);
}
