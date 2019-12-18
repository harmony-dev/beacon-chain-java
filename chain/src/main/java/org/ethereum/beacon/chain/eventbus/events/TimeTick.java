package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.core.types.Time;

public class TimeTick implements Event<Time> {

  public static TimeTick wrap(Time time) {
    return new TimeTick(time);
  }

  private Time time;

  public TimeTick(Time time) {
    this.time = time;
  }

  @Override
  public Time getValue() {
    return time;
  }
}
