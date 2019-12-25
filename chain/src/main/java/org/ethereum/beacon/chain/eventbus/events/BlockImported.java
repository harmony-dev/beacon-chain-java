package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public class BlockImported extends AbstractBlockEvent {

  public static BlockImported wrap(SignedBeaconBlock block) {
    return new BlockImported(block);
  }

  public BlockImported(SignedBeaconBlock block) {
    super(block);
  }
}
