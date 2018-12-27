package org.ethereum.beacon.consensus;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.consensus.types.Score;

public interface ScoreFunction {

  Score apply(BeaconBlock block, BeaconState postState);
}
