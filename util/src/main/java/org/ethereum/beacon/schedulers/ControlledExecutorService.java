package org.ethereum.beacon.schedulers;

import java.util.concurrent.ScheduledExecutorService;


public interface ControlledExecutorService extends ScheduledExecutorService {

  void setCurrentTime(long currentTime);

  long getNextScheduleTime();

}
