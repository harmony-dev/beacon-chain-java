package org.ethereum.beacon.core;

import org.ethereum.beacon.core.BeaconChainSpec.Genesis;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;

/** A class holding various utility methods to work with {@link BeaconBlock}. */
public abstract class BeaconBlocks {
  private BeaconBlocks() {}

  /**
   * Creates an instance of Genesis block.
   *
   * <p><strong>Note:</strong> it assumed that {@link BeaconBlock#stateRoot} will be set later on,
   * hence, it's set to {@link Hash32#ZERO}.
   *
   * @return a genesis block.
   */
  public static BeaconBlock createGenesis() {
    return new BeaconBlock(
        Genesis.SLOT,
        Hash32.ZERO,
        Hash32.ZERO,
        Hash32.ZERO,
        Hash32.ZERO,
        Bytes96.ZERO,
        BeaconBlockBody.EMPTY);
  }
}
