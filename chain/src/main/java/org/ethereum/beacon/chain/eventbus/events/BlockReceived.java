package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.BeaconBlock;

public class BlockReceived extends AbstractBlockEvent {

  public static BlockReceived wrap(BeaconBlock block) {
    return new BlockReceived(block);
  }

  public BlockReceived(BeaconBlock block) {
    super(block);
  }
}
