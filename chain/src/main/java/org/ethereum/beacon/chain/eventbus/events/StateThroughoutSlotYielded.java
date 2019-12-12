package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.chain.processor.BeaconStateAtTheTip;

public class StateThroughoutSlotYielded implements Event<BeaconStateAtTheTip> {

  public static StateThroughoutSlotYielded wrap(BeaconStateAtTheTip state) {
    return new StateThroughoutSlotYielded(state);
  }

  private final BeaconStateAtTheTip state;

  public StateThroughoutSlotYielded(BeaconStateAtTheTip state) {
    this.state = state;
  }

  @Override
  public BeaconStateAtTheTip getValue() {
    return state;
  }
}
