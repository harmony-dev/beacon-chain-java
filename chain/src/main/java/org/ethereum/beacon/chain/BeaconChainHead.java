package org.ethereum.beacon.chain;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

public class BeaconChainHead {

  private final BeaconBlock block;
  private final BeaconState state;

  public BeaconChainHead(BeaconBlock block, BeaconState state) {
    this.block = block;
    this.state = state;
  }

  public BeaconBlock getBlock() {
    return block;
  }

  public BeaconState getState() {
    return state;
  }
}
