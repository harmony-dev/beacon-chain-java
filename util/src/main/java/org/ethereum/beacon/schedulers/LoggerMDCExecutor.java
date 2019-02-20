package org.ethereum.beacon.schedulers;

import java.util.concurrent.Executor;
import org.apache.logging.log4j.ThreadContext;

public class LoggerMDCExecutor implements Executor {

  private final String mdcKey;
  private final String mdcValue;
  private final Executor delegateExecutor;

  public LoggerMDCExecutor(String mdcKey, String mdcValue,
      Executor delegateExecutor) {
    this.mdcKey = mdcKey;
    this.mdcValue = mdcValue;
    this.delegateExecutor = delegateExecutor;
  }

  public LoggerMDCExecutor(String mdcKey, String mdcValue) {
    this(mdcKey, mdcValue, Runnable::run);
  }

  @Override
  public void execute(Runnable command) {
    String oldValue = ThreadContext.get(mdcKey);
    ThreadContext.put(mdcKey, mdcValue);
    delegateExecutor.execute(command);
    if (oldValue == null) {
      ThreadContext.remove(mdcKey);
    } else {
      ThreadContext.put(mdcKey, oldValue);
    }
  }
}
