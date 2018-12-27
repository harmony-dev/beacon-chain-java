package org.ethereum.beacon.core;

import tech.pegasys.artemis.ethereum.core.Hash32;

public abstract class BeaconBlocks {
  private BeaconBlocks() {}

  public static BeaconBlock createGenesis() {
    return new BeaconBlock(Hash32.ZERO);
  }
}
