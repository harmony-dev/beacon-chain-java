package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.BeaconBlock;

public class BlockImported extends AbstractBlockEvent {

  public static BlockImported wrap(BeaconBlock block) {
    return new BlockImported(block);
  }

  public BlockImported(BeaconBlock block) {
    super(block);
  }
}
