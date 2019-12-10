package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;

public class ObservableStateUpdated implements Event<ObservableBeaconState> {

  public static ObservableStateUpdated wrap(ObservableBeaconState state) {
    return new ObservableStateUpdated(state);
  }

  private final ObservableBeaconState state;

  public ObservableStateUpdated(ObservableBeaconState state) {
    this.state = state;
  }

  @Override
  public ObservableBeaconState getValue() {
    return state;
  }
}
