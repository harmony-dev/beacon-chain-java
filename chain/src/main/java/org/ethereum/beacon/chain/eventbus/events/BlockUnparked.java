package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.BeaconBlock;

public class BlockUnparked extends AbstractBlockEvent {

  public static BlockUnparked wrap(BeaconBlock block) {
    return new BlockUnparked(block);
  }

  public BlockUnparked(BeaconBlock block) {
    super(block);
  }
}
