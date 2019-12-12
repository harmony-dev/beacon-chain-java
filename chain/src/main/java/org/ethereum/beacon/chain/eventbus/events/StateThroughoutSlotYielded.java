package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;

public class StateThroughoutSlotYielded implements Event<ObservableBeaconState> {

  public static StateThroughoutSlotYielded wrap(ObservableBeaconState state) {
    return new StateThroughoutSlotYielded(state);
  }

  private final ObservableBeaconState state;

  public StateThroughoutSlotYielded(ObservableBeaconState state) {
    this.state = state;
  }

  @Override
  public ObservableBeaconState getValue() {
    return state;
  }
}
