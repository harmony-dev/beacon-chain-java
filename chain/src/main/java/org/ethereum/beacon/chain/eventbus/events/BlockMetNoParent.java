package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public class BlockMetNoParent extends AbstractBlockEvent {

  public static BlockMetNoParent wrap(SignedBeaconBlock block) {
    return new BlockMetNoParent(block);
  }

  public BlockMetNoParent(SignedBeaconBlock block) {
    super(block);
  }
}
