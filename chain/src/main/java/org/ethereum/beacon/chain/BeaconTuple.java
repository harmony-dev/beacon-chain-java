package org.ethereum.beacon.chain;

import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public class BeaconTuple {

  private final SignedBeaconBlock block;
  private final BeaconStateEx state;

  BeaconTuple(SignedBeaconBlock block, BeaconStateEx state) {
    this.block = block;
    this.state = state;
  }

  public static BeaconTuple of(SignedBeaconBlock block, BeaconStateEx state) {
    return new BeaconTuple(block, state);
  }

  public SignedBeaconBlock getBlock() {
    return block;
  }

  public BeaconStateEx getState() {
    return state;
  }
}
