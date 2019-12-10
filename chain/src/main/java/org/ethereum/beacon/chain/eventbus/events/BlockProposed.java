package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.BeaconBlock;

public class BlockProposed extends AbstractBlockEvent {

  public static BlockProposed wrap(BeaconBlock block) {
    return new BlockProposed(block);
  }

  public BlockProposed(BeaconBlock block) {
    super(block);
  }
}
