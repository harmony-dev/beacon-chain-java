package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.BeaconBlock;

public class BlockConsiderationDelayed extends AbstractBlockEvent {

  public static BlockConsiderationDelayed wrap(BeaconBlock block) {
    return new BlockConsiderationDelayed(block);
  }

  public BlockConsiderationDelayed(BeaconBlock block) {
    super(block);
  }
}
