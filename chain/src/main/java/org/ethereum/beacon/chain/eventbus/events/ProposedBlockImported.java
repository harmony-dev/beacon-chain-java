package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.BeaconBlock;

public class ProposedBlockImported extends AbstractBlockEvent {

  public static ProposedBlockImported wrap(BeaconBlock block) {
    return new ProposedBlockImported(block);
  }

  public ProposedBlockImported(BeaconBlock block) {
    super(block);
  }
}
