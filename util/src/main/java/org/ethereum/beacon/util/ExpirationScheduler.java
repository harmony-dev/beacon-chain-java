package org.ethereum.beacon.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Schedules `runnable` in delay which is set by constructor. When runnable is renewed by putting it
 * in map again, old task is cancelled and removed. Task are equalled by the <Key>
 */
public class ExpirationScheduler<Key> {
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final long delay;
  private final TimeUnit timeUnit;
  private Map<Key, ScheduledFuture> expirationTasks = new ConcurrentHashMap<>();

  public ExpirationScheduler(long delay, TimeUnit timeUnit) {
    this.delay = delay;
    this.timeUnit = timeUnit;
  }

  public void put(Key key, Runnable runnable) {
    synchronized (this) {
      if (expirationTasks.containsKey(key)) {
        expirationTasks.remove(key).cancel(true);
      }
    }
    ScheduledFuture future =
        scheduler.schedule(
            () -> {
              runnable.run();
              expirationTasks.remove(key);
            },
            delay,
            timeUnit);
    expirationTasks.put(key, future);
  }
}
