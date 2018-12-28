package org.ethereum.beacon.core;

import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

/** A class holding various utility methods to work with {@link BeaconBlock}. */
public abstract class BeaconBlocks {
  private BeaconBlocks() {}

  /**
   * Creates an instance of Genesis block.
   *
   * @return a genesis block.
   */
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
