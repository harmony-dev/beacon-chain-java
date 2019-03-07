package org.ethereum.beacon.schedulers;

import java.util.function.Supplier;

public interface TimeControlled {

  void setTimeSupplier(Supplier<Long> globalClock);

  void updateToCurrentTime();

  long getNextEventTime();
}
