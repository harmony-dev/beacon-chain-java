package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public class ProposedBlockImported extends AbstractBlockEvent {

  public static ProposedBlockImported wrap(SignedBeaconBlock block) {
    return new ProposedBlockImported(block);
  }

  public ProposedBlockImported(SignedBeaconBlock block) {
    super(block);
  }
}
