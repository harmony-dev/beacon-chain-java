package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public class BlockReceived extends AbstractBlockEvent {

  public static BlockReceived wrap(SignedBeaconBlock block) {
    return new BlockReceived(block);
  }

  public BlockReceived(SignedBeaconBlock block) {
    super(block);
  }
}
