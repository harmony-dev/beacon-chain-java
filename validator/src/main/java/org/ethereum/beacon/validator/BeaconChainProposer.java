package org.ethereum.beacon.validator;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.pending.ObservableBeaconState;
import org.ethereum.beacon.randao.Randao;

public interface BeaconChainProposer {

  BeaconBlock propose(ObservableBeaconState state, Randao randao);
}
