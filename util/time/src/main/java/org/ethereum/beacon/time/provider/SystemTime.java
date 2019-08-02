package org.ethereum.beacon.time.provider;

import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Time Provider uses system time
 */
public class SystemTime implements TimeProvider {
  private static final int MILLIS_IN_SEC = 1000;
  private final SimpleProcessor<Time> timeProcessor;
  private final AtomicLong latest = new AtomicLong(-1);

  public SystemTime(Scheduler events, Scheduler worker) {
    this.timeProcessor = new SimpleProcessor<Time>(events, "TimeProvider.system");
    worker.executeR(
        () -> {
          while (!Thread.currentThread().isInterrupted()) {
            emitTime();
          }
        });
  }

  private void emitTime() {
    long currentSec = Instant.now().getEpochSecond();
    if (currentSec > latest.get()) {
      System.out.println("Time: " + currentSec);
      timeProcessor.onNext(Time.of(currentSec));
      latest.set(currentSec);
    }
    long currentMs = Instant.now().getLong(ChronoField.MILLI_OF_SECOND);
    try {
      Thread.sleep(MILLIS_IN_SEC - currentMs);
    } catch (InterruptedException e) {
      throw new RuntimeException("SystemTime stream thread interrupted!", e);
    }
  }

  @Override
  public Publisher<Time> getTimeStream() {
    return timeProcessor;
  }
}
