package org.ethereum.beacon.core;

import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconState {

  public Hash32 getHash() {
    return Hash32.ZERO;
  }

  public static BeaconState createEmpty() {
    return new BeaconState();
  }
}
