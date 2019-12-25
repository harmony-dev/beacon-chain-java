package org.ethereum.beacon.chain;

import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public class BeaconTuple {

  private final SignedBeaconBlock signedBlock;
  private final BeaconStateEx state;

  BeaconTuple(SignedBeaconBlock signedBlock, BeaconStateEx state) {
    this.signedBlock = signedBlock;
    this.state = state;
  }

  public static BeaconTuple of(SignedBeaconBlock block, BeaconStateEx state) {
    return new BeaconTuple(block, state);
  }

  public SignedBeaconBlock getSignedBlock() {
    return signedBlock;
  }

  public BeaconStateEx getState() {
    return state;
  }
}
