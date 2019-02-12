package org.ethereum.beacon.schedulers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DefaultSchedulers extends Schedulers {
  private static final int BLOCKING_THREAD_COUNT = 128;

  private final Scheduler cpuHeavy =
      new ExecutorScheduler(
          Executors.newScheduledThreadPool(
              Runtime.getRuntime().availableProcessors(),
              new ThreadFactoryBuilder()
                  .setDaemon(true)
                  .setNameFormat("Schedulers-cpuHeavy-%d")
                  .build()));
  private final Scheduler blocking =
      new ExecutorScheduler(
          Executors.newScheduledThreadPool(
              BLOCKING_THREAD_COUNT,
              new ThreadFactoryBuilder()
                  .setDaemon(true)
                  .setNameFormat("Schedulers-blocking-%d")
                  .build()));

  private final ScheduledExecutorService eventsExecutor =
      Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Schedulers-events").build());
  private final Scheduler events = new ExecutorScheduler(eventsExecutor);
  private final reactor.core.scheduler.Scheduler reactorEvents =
      reactor.core.scheduler.Schedulers.fromExecutorService(eventsExecutor);

  @Override
  public Scheduler cpuHeavy() {
    return cpuHeavy;
  }

  @Override
  public Scheduler blocking() {
    return blocking;
  }

  @Override
  public Scheduler events() {

    return blocking;
  }

  @Override
  public reactor.core.scheduler.Scheduler reactorEvents() {
    return reactorEvents;
  }

  @Override
  public Scheduler newSingleThreadDaemon(String threadName) {
    return new ExecutorScheduler(
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Schedulers-" + threadName)
                .build()));
  }

  @Override
  public Scheduler newParallelDaemon(String threadNamePattern, int threadPoolCount) {
    return new ExecutorScheduler(
        Executors.newScheduledThreadPool(
            threadPoolCount,
            new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Schedulers-" + threadNamePattern)
                .build()));
  }
}
