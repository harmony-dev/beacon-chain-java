package org.ethereum.beacon.schedulers;

/**
 * The collection of standard Schedulers, Scheduler factory and system time supplier
 *
 * For debugging and testing the default <code>Schedulers</code> instance can be replaced
 * with appropriate one
 */
public abstract class Schedulers {
  private static Schedulers current;

  public static Schedulers get() {
    return current;
  }

  public static void set(Schedulers newStaticSchedulers) {
    current = newStaticSchedulers;
  }

  public static void resetToDefault() {

  }

  public long getCurrentTime() {
    return System.currentTimeMillis();
  }

  public abstract Scheduler cpuHeavy();

  public abstract Scheduler blocking();

  public abstract Scheduler events();

  public abstract reactor.core.scheduler.Scheduler reactorEvents();

  public abstract Scheduler newSingleThreadDaemon(String threadName);

  public abstract Scheduler newParallelDaemon(String threadName, int threadPoolCount);
}
