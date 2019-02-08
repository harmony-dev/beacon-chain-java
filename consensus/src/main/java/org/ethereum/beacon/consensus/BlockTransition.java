package org.ethereum.beacon.consensus;

import org.ethereum.beacon.core.BeaconBlock;

public interface BlockTransition<State> {

  State apply(State source, BeaconBlock input);
}
