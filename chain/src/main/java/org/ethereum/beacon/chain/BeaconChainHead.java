package org.ethereum.beacon.chain;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public class BeaconChainHead {

  private final BeaconTuple tuple;

  public BeaconChainHead(BeaconTuple tuple) {
    this.tuple = tuple;
  }

  public static BeaconChainHead of(BeaconTuple tuple) {
    return new BeaconChainHead(tuple);
  }

  public BeaconTuple getTuple() {
    return tuple;
  }

  public SignedBeaconBlock getBlock() {
    return tuple.getBlock();
  }

  public BeaconState getState() {
    return tuple.getState();
  }
}
