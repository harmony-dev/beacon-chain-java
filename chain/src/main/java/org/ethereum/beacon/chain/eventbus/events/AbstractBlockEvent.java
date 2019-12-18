package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.core.BeaconBlock;

abstract class AbstractBlockEvent implements Event<BeaconBlock> {

  private final BeaconBlock block;

  protected AbstractBlockEvent(BeaconBlock block) {
    this.block = block;
  }

  @Override
  public BeaconBlock getValue() {
    return block;
  }
}
