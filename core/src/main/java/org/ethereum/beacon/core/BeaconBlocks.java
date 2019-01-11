package org.ethereum.beacon.core;

import org.ethereum.beacon.core.spec.ChainSpec;
import tech.pegasys.artemis.ethereum.core.Hash32;

/** A class holding various utility methods to work with {@link BeaconBlock}. */
public abstract class BeaconBlocks {
  private BeaconBlocks() {}

  /**
   * Creates an instance of Genesis block.
   *
   * <p><strong>Note:</strong> it assumed that {@link BeaconBlock#stateRoot} will be set later on,
   * hence, it's set to {@link Hash32#ZERO}.
   *
   * @param chainSpec beacon chain spec.
   * @return a genesis block.
   */
  public static BeaconBlock createGenesis(ChainSpec chainSpec) {
    return BeaconBlock.Builder.createEmpty()
        .withSlot(chainSpec.getGenesisSlot())
        .withParentRoot(Hash32.ZERO)
        .withStateRoot(Hash32.ZERO)
        .withRandaoReveal(Hash32.ZERO)
        .withDepositRoot(Hash32.ZERO)
        .withSignature(chainSpec.getEmptySignature())
        .withBody(BeaconBlockBody.EMPTY)
        .build();
  }
}
