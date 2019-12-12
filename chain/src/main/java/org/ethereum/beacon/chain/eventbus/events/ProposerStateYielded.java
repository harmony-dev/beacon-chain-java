package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;

public class ProposerStateYielded implements Event<ObservableBeaconState> {

  public static ProposerStateYielded wrap(ObservableBeaconState state) {
    return new ProposerStateYielded(state);
  }

  private final ObservableBeaconState state;

  public ProposerStateYielded(ObservableBeaconState state) {
    this.state = state;
  }

  @Override
  public ObservableBeaconState getValue() {
    return state;
  }
}
