package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.consensus.transition.BeaconStateEx;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

public class BeaconTuple {

  private final BeaconBlock block;
  private final BeaconState state;

  BeaconTuple(BeaconBlock block, BeaconState state) {
    this.block = block;
    this.state = state;
  }

  public static BeaconTuple of(BeaconBlock block, BeaconState state) {
    return new BeaconTuple(block, state);
  }

  public static BeaconTuple of(BeaconBlock block, BeaconStateEx state) {
    return new BeaconTuple(block, state.getCanonicalState());
  }

  public BeaconBlock getBlock() {
    return block;
  }

  public BeaconState getState() {
    return state;
  }
}
