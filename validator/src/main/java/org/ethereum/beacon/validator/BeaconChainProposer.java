package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.core.BeaconBlock;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface BeaconChainProposer {

  BeaconBlock propose(ObservableBeaconState state, Hash32 depositRoot);
}
