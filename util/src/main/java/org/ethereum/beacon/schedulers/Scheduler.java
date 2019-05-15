package org.ethereum.beacon.schedulers;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Analog for standard <code>ScheduledExecutorService</code>
 */
public interface Scheduler {

  <T> CompletableFuture<T> execute(Callable<T> task);

  <T> CompletableFuture<T> executeWithDelay(Duration delay, Callable<T> task);

  CompletableFuture<Void> executeAtFixedRate(Duration initialDelay, Duration period, RunnableEx task);

  default CompletableFuture<Void> execute(RunnableEx task) {
    return execute(() -> {task.run(); return null;});
  }

  default CompletableFuture<Void> executeWithDelay(Duration delay, RunnableEx task) {
    return executeWithDelay(delay, () -> {task.run(); return null;});
  }

  default <C> CompletableFuture<C> orTimeout(CompletableFuture<C> future, Duration futureTimeout, Supplier<Exception> exceptionSupplier) {
    return (CompletableFuture<C>) CompletableFuture.anyOf(
        future,
        executeWithDelay(futureTimeout,
            () -> {throw exceptionSupplier.get();}));
  }
}
