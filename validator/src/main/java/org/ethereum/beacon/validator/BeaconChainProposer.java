package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.core.BeaconBlock;

public interface BeaconChainProposer {

  BeaconBlock propose(ObservableBeaconState state);
}
