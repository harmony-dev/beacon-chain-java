package org.ethereum.beacon.schedulers;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

public class ControlledSchedulersImpl extends AbstractSchedulers implements ControlledSchedulers {

  private final List<ControlledExecutorService> controlledExecutors = new CopyOnWriteArrayList<>();
  private long currentTime = 0;

  @Override
  public long getCurrentTime() {
    return currentTime;
  }

  @Override
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
      if (time < currentTime) {
        throw new IllegalStateException("Invalid task time: " + time + " < " + currentTime);
      }
      currentTime = time;
      nextSchedulerToRun.setCurrentTime(currentTime);
    }
    currentTime = newTime;
    controlledExecutors.forEach(e -> e.setCurrentTime(newTime));
  }

  @Override
  protected Scheduler createExecutorScheduler(ScheduledExecutorService executorService) {
    return new ErrorHandlingScheduler(new ExecutorScheduler(executorService), e -> e.printStackTrace());
  }

  @Override
  protected ScheduledExecutorService createExecutor(String namePattern, int threads) {
    ControlledExecutorService service = new ControlledExecutorServiceImpl(createDelegateExecutor());
    controlledExecutors.add(service);
    service.setCurrentTime(currentTime);
    return service;
  }

  protected Executor createDelegateExecutor() {
    return Runnable::run;
  }
}
