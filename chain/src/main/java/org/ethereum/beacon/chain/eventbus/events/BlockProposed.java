package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public class BlockProposed extends AbstractBlockEvent {

  public static BlockProposed wrap(SignedBeaconBlock block) {
    return new BlockProposed(block);
  }

  public BlockProposed(SignedBeaconBlock block) {
    super(block);
  }
}
