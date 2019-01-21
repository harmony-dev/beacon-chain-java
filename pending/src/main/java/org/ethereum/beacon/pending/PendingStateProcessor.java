package org.ethereum.beacon.pending;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

public interface PendingStateProcessor {

  PendingState processBlock(BeaconBlock block, BeaconState state);
}
