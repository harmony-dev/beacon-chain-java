package org.ethereum.beacon.core;

import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconState implements Hashable<Hash32> {
  public static final BeaconState EMPTY = new BeaconState();

  @Override
  public Hash32 getHash() {
    return Hash32.ZERO;
  }
}
