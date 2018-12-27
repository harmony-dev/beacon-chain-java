package org.ethereum.beacon.consensus;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

public interface StateTransition<S> {

  BeaconState applyBlock(BeaconBlock block, S state);
}
