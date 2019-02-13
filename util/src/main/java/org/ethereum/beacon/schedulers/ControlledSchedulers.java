package org.ethereum.beacon.schedulers;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

public class ControlledSchedulers extends Schedulers {

  private final List<ControlledExecutorService> controlledExecutors = new CopyOnWriteArrayList<>();
  private long currentTime = 0;

  @Override
  public long getCurrentTime() {
    return currentTime;
  }

  public void setCurrentTime(long newTime) {
    assert newTime >= currentTime;
    while (true) {
      Optional<ControlledExecutorService> nextSchedulerToRunOpt =
          controlledExecutors.stream()
              .min(Comparator.comparingLong(ex -> ex.getNextScheduleTime()));
      if (!nextSchedulerToRunOpt.isPresent()) {
        break;
      }
      ControlledExecutorService nextSchedulerToRun = nextSchedulerToRunOpt.get();
      long time = nextSchedulerToRun.getNextScheduleTime();
      if (time > newTime) {
        break;
      }
      currentTime = time;
      nextSchedulerToRun.setCurrentTime(currentTime);
    }
    currentTime = newTime;
  }

  @Override
  protected ScheduledExecutorService createExecutor(String namePattern, int threads) {
    ControlledExecutorService service = new ControlledExecutorServiceImpl();
    controlledExecutors.add(service);
    return service;
  }
}
