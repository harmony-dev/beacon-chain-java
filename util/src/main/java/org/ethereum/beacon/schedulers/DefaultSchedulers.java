package org.ethereum.beacon.schedulers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class DefaultSchedulers extends Schedulers {

  private Consumer<Throwable> errorHandler =
      t -> {
        System.err.println("ErrorHandlingScheduler (default error handler):");
        t.printStackTrace();
      };
  private volatile boolean started;

  public void setErrorHandler(Consumer<Throwable> errorHandler) {
    if (started) {
      throw new IllegalStateException("ErrorHandler should be set up prior to any other calls");
    }
    this.errorHandler = errorHandler;
  }

  @Override
  protected Scheduler createExecutorScheduler(ScheduledExecutorService executorService) {
    return new ErrorHandlingScheduler(new ExecutorScheduler(executorService), errorHandler);
  }

  @Override
  protected ScheduledExecutorService createExecutor(String namePattern, int threads) {
    started = true;
    return Executors.newScheduledThreadPool(
        threads, new ThreadFactoryBuilder().setDaemon(true).setNameFormat(namePattern).build());
  }
}
