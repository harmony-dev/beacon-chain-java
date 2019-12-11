package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;

public class AttesterStateUpdated implements Event<ObservableBeaconState> {

  public static AttesterStateUpdated wrap(ObservableBeaconState state) {
    return new AttesterStateUpdated(state);
  }

  private final ObservableBeaconState state;

  public AttesterStateUpdated(ObservableBeaconState state) {
    this.state = state;
  }

  @Override
  public ObservableBeaconState getValue() {
    return state;
  }
}
