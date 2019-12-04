package org.ethereum.beacon.chain;

import java.time.Duration;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;

public class TimeTicker implements Ticker<Time> {

  private final SimpleProcessor<Time> timeStream;
  private final Schedulers schedulers;

  public TimeTicker(Schedulers schedulers) {
    this.schedulers = schedulers;
    this.timeStream = new SimpleProcessor<>(schedulers.events(), "TimeTicker");
  }

  @Override
  public void start() {
    long currentTime = schedulers.getCurrentTime();
    schedulers.newSingleThreadDaemon("time-ticker").executeAtFixedRate(
        Duration.ofMillis(currentTime - currentTime % 1000 + 1000),
        Duration.ofSeconds(1),
        () -> timeStream.onNext(Time.of(schedulers.getCurrentTime()))
    );
  }

  @Override
  public void stop() {}

  @Override
  public Publisher<Time> getTickerStream() {
    return timeStream;
  }
}
