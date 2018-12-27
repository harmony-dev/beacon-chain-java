package org.ethereum.beacon.core;

import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconBlock {

  private final Hash32 stateRoot;

  BeaconBlock(Hash32 stateRoot) {
    this.stateRoot = stateRoot;
  }

  public Hash32 getStateRoot() {
    return stateRoot;
  }

  public Hash32 getHash() {
    return Hash32.ZERO;
  }

  public BeaconBlock withStateRoot(Hash32 hash) {
    return new BeaconBlock(hash);
  }
}
