package org.ethereum.beacon.core;

import static java.util.Collections.emptyList;

import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
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
   * @param specConstants beacon chain spec.
   * @return a genesis block.
   */
  public static BeaconBlock createGenesis(SpecConstants specConstants) {
    BeaconBlockBody body =
        new BeaconBlockBody(
            specConstants.getEmptySignature(),
            Eth1Data.EMPTY,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList());

    return BeaconBlock.Builder.createEmpty()
        .withSlot(specConstants.getGenesisSlot())
        .withParentRoot(Hash32.ZERO)
        .withStateRoot(Hash32.ZERO)
        .withSignature(specConstants.getEmptySignature())
        .withBody(body)
        .build();
  }
}
