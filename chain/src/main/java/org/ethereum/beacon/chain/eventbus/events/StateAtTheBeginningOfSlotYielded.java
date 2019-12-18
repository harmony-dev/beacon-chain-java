package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;

public class StateAtTheBeginningOfSlotYielded implements Event<ObservableBeaconState> {

  public static StateAtTheBeginningOfSlotYielded wrap(ObservableBeaconState state) {
    return new StateAtTheBeginningOfSlotYielded(state);
  }

  private final ObservableBeaconState state;

  public StateAtTheBeginningOfSlotYielded(ObservableBeaconState state) {
    this.state = state;
  }

  @Override
  public ObservableBeaconState getValue() {
    return state;
  }
}
