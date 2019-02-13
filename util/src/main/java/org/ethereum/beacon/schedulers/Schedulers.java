package org.ethereum.beacon.schedulers;

import java.util.concurrent.ScheduledExecutorService;

/**
 * The collection of standard Schedulers, Scheduler factory and system time supplier
 *
 * For debugging and testing the default <code>Schedulers</code> instance can be replaced
 * with appropriate one
 */
public abstract class Schedulers {
  private static final int BLOCKING_THREAD_COUNT = 128;

  private static Schedulers current;

  public static Schedulers get() {
    if (current == null) {
      resetToDefault();
    }
    return current;
  }

  public static void set(Schedulers newStaticSchedulers) {
    current = newStaticSchedulers;
  }

  public static void resetToDefault() {
    current = new DefaultSchedulers();
  }

  private Scheduler cpuHeavyScheduler;
  private Scheduler blockingScheduler;
  private Scheduler eventsScheduler;
  private reactor.core.scheduler.Scheduler eventsReactorScheduler;
  private ScheduledExecutorService eventsExecutor;

  public long getCurrentTime() {
    return System.currentTimeMillis();
  }

  protected abstract ScheduledExecutorService createExecutor(String namePattern, int threads);

  public Scheduler cpuHeavy() {
    if (cpuHeavyScheduler == null) {
      synchronized (this) {
        if (cpuHeavyScheduler == null) {
          cpuHeavyScheduler = createCpuHeavy();
        }
      }
    }
    return cpuHeavyScheduler;
  }

  protected Scheduler createCpuHeavy() {
    return new ExecutorScheduler(createCpuHeavyExecutor());
  }

  protected ScheduledExecutorService createCpuHeavyExecutor() {
    return createExecutor("Schedulers-cpuHeavy-%d", Runtime.getRuntime().availableProcessors());
  }

  public Scheduler blocking() {
    if (blockingScheduler == null) {
      synchronized (this) {
        if (blockingScheduler == null) {
          blockingScheduler = createBlocking();
        }
      }
    }
    return blockingScheduler;
  }

  protected Scheduler createBlocking() {
    return new ExecutorScheduler(createBlockingExecutor());
  }

  protected ScheduledExecutorService createBlockingExecutor() {
    return createExecutor("Schedulers-blocking-%d", BLOCKING_THREAD_COUNT);
  }

  public Scheduler events() {
    if (eventsScheduler == null) {
      synchronized (this) {
        if (eventsScheduler == null) {
          eventsScheduler = createEvents();
        }
      }
    }
    return eventsScheduler;
  }

  protected Scheduler createEvents() {
    return new ExecutorScheduler(getEventsExecutor());
  }

  protected ScheduledExecutorService getEventsExecutor() {
    if (eventsExecutor == null) {
      eventsExecutor = createExecutor("Schedulers-events", 1);
    }
    return eventsExecutor;
  }

  public reactor.core.scheduler.Scheduler reactorEvents() {
    if (eventsReactorScheduler == null) {
      synchronized (this) {
        if (eventsReactorScheduler == null) {
          eventsReactorScheduler = createReactorEvents();
        }
      }
    }
    return eventsReactorScheduler;
  }

  protected reactor.core.scheduler.Scheduler createReactorEvents() {
    return reactor.core.scheduler.Schedulers.fromExecutor(getEventsExecutor());
  }

  public Scheduler newSingleThreadDaemon(String threadName) {
    return new ExecutorScheduler(createExecutor(threadName, 1));
  }

  public Scheduler newParallelDaemon(String threadNamePattern, int threadPoolCount) {
    return new ExecutorScheduler(createExecutor(threadNamePattern, threadPoolCount));
  }
}
