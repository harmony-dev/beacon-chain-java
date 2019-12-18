package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.BeaconBlock;

public class BlockMetNoParent extends AbstractBlockEvent {

  public static BlockMetNoParent wrap(BeaconBlock block) {
    return new BlockMetNoParent(block);
  }

  public BlockMetNoParent(BeaconBlock block) {
    super(block);
  }
}
