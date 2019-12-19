package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

abstract class AbstractBlockEvent implements Event<SignedBeaconBlock> {

  private final SignedBeaconBlock block;

  protected AbstractBlockEvent(SignedBeaconBlock block) {
    this.block = block;
  }

  @Override
  public SignedBeaconBlock getValue() {
    return block;
  }
}
