package org.ethereum.beacon.core;

import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

public abstract class BeaconBlocks {
  private BeaconBlocks() {}

  public static BeaconBlock createGenesis() {
    return new BeaconBlock(
        UInt64.ZERO,
        Hash32.ZERO,
        Hash32.ZERO,
        Hash32.ZERO,
        Hash32.ZERO,
        Bytes96.ZERO,
        BeaconBlockBody.EMPTY);
  }
}
