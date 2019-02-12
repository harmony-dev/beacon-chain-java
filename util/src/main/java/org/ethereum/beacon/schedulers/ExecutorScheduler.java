package org.ethereum.beacon.schedulers;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ExecutorScheduler implements Scheduler {

  private final ScheduledExecutorService executorService;

  public ExecutorScheduler(ScheduledExecutorService executorService) {
    this.executorService = executorService;
  }

  @Override
  public <T> CompletableFuture<T> execute(CallableEx<T> task) {
    CompletableFuture<T> future = new CompletableFuture<>();
    executorService.execute(() -> {
      try {
        future.complete(task.call());
      } catch (Throwable t) {
        future.completeExceptionally(t);
      }
    });
    return future;
  }

  @Override
  public <T> CompletableFuture<T> executeWithDelay(Duration delay, CallableEx<T> task) {
    CompletableFuture<T> future = new CompletableFuture<>();
    executorService.schedule(() -> {
      try {
        future.complete(task.call());
      } catch (Throwable t) {
        future.completeExceptionally(t);
      }
    }, delay.toMillis(), TimeUnit.MILLISECONDS);
    return future;
  }

  @Override
  public CompletableFuture<Void> executeAtFixedRate(Duration initialDelay, Duration period,
      RunnableEx task) {

    ScheduledFuture<?>[] scheduledFuture = new ScheduledFuture[1];
    CompletableFuture<Void> ret = new CompletableFuture<Void>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return scheduledFuture[0].cancel(mayInterruptIfRunning);
      }
    };
    scheduledFuture[0] = executorService.scheduleAtFixedRate(() -> {
      try {
        task.run();
      } catch (Throwable e) {
        ret.completeExceptionally(e);
        throw new RuntimeException(e);
      }
    }, initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);

    return ret;
  }
}
