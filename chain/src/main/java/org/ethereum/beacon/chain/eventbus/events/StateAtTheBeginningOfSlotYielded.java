package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.chain.processor.BeaconStateAtTheTip;

public class StateAtTheBeginningOfSlotYielded implements Event<BeaconStateAtTheTip> {

  public static StateAtTheBeginningOfSlotYielded wrap(BeaconStateAtTheTip state) {
    return new StateAtTheBeginningOfSlotYielded(state);
  }

  private final BeaconStateAtTheTip state;

  public StateAtTheBeginningOfSlotYielded(BeaconStateAtTheTip state) {
    this.state = state;
  }

  @Override
  public BeaconStateAtTheTip getValue() {
    return state;
  }
}
