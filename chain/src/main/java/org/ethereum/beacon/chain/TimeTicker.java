package org.ethereum.beacon.chain;

import java.time.Duration;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

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
    long startTime = currentTime - currentTime % 1000 + 1000;
    Time start = Time.of(startTime / 1000);
    Flux.interval(
            Duration.ofMillis(startTime - currentTime),
            Duration.ofSeconds(1),
            schedulers.events().toReactor())
        .map(start::plus)
        .subscribe(timeStream::onNext);
  }

  @Override
  public void stop() {}

  @Override
  public Publisher<Time> getTickerStream() {
    return timeStream;
  }
}
