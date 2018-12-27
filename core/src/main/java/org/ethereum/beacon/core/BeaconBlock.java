package org.ethereum.beacon.core;

import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconBlock implements Hashable {

  private final Hash32 stateRoot;

  BeaconBlock(Hash32 stateRoot) {
    this.stateRoot = stateRoot;
  }

  public Hash32 getStateRoot() {
    return stateRoot;
  }

  @Override
  public Hash32 getHash() {
    return Hash32.ZERO;
  }

  public Hash32 getParentRoot() {
    return Hash32.ZERO;
  }

  public boolean isParentOf(BeaconBlock other) {
    return true;
  }

  public BeaconBlock withStateRoot(Hash32 hash) {
    return new BeaconBlock(hash);
  }
}
