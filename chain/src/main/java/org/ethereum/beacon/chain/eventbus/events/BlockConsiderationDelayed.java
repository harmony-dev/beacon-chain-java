package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public class BlockConsiderationDelayed extends AbstractBlockEvent {

  public static BlockConsiderationDelayed wrap(SignedBeaconBlock block) {
    return new BlockConsiderationDelayed(block);
  }

  public BlockConsiderationDelayed(SignedBeaconBlock block) {
    super(block);
  }
}
