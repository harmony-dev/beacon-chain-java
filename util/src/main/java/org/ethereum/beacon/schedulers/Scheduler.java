package org.ethereum.beacon.schedulers;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Analog for standard <code>ScheduledExecutorService</code>
 */
public interface Scheduler {

  <T> CompletableFuture<T> execute(CallableEx<T> task);

  <T> CompletableFuture<T> executeWithDelay(Duration delay, CallableEx<T> task);

  CompletableFuture<Void> executeAtFixedRate(Duration initialDelay, Duration period, RunnableEx task);

  default CompletableFuture<Void> execute(RunnableEx task) {
    return execute(() -> {task.run(); return null;});
  }

  default CompletableFuture<Void> executeWithDelay(Duration delay, RunnableEx task) {
    return executeWithDelay(delay, () -> {task.run(); return null;});
  }
}
