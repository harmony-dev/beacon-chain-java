package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.BeaconBlock;

public class BlockDequeued extends AbstractBlockEvent {

  public static BlockDequeued wrap(BeaconBlock block) {
    return new BlockDequeued(block);
  }

  public BlockDequeued(BeaconBlock block) {
    super(block);
  }
}
