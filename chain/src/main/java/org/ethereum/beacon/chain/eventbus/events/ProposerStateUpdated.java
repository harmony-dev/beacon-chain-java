package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;

public class ProposerStateUpdated implements Event<ObservableBeaconState> {

  public static ProposerStateUpdated wrap(ObservableBeaconState state) {
    return new ProposerStateUpdated(state);
  }

  private final ObservableBeaconState state;

  public ProposerStateUpdated(ObservableBeaconState state) {
    this.state = state;
  }

  @Override
  public ObservableBeaconState getValue() {
    return state;
  }
}
