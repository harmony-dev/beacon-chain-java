package org.ethereum.beacon.schedulers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class TimeControlledGroup implements TimeControlled, Supplier<Long> {
  private final List<? extends TimeControlled> children;
  private Supplier<Long> globalClock;
  private Long curTime;

  public TimeControlledGroup(List<? extends TimeControlled> children) {
    this.children = children;
  }

  @Override
  public void setTimeSupplier(Supplier<Long> globalClock) {
    this.globalClock = globalClock;
  }

  @Override
  public void setTime(long newTime) {
    while (true) {
      TimeControlled minChild = Collections
          .min(children, Comparator.comparing(TimeControlled::getNextEventTime));
      long minTime = minChild.getNextEventTime();
      if (minTime > newTime) {
        break;
      }
    }
  }

  @Override
  public Long get() {
    return curTime != null ? curTime : globalClock.get();
  }

  @Override
  public long getNextEventTime() {
    return 0;
  }
}
