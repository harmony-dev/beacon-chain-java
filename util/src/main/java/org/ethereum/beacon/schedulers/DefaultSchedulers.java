package org.ethereum.beacon.schedulers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DefaultSchedulers extends Schedulers {

  @Override
  protected ScheduledExecutorService createExecutor(String namePattern, int threads) {
    return Executors.newScheduledThreadPool(
        threads, new ThreadFactoryBuilder().setDaemon(true).setNameFormat(namePattern).build());
  }
}
