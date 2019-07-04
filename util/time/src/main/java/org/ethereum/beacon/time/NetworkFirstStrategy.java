package org.ethereum.beacon.time;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.ethereum.beacon.time.provider.NetworkTime;
import org.ethereum.beacon.time.provider.StatisticsTime;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Strategy which prioritize {@link NetworkTime} but if the delta between {@link NetworkTime} and
 * {@link StatisticsTime} goes above allowedDelta, it uses {@link StatisticsTime} as time provider.
 */
public class NetworkFirstStrategy implements TimeStrategy {
  private final SimpleProcessor<Time> timeProcessor;
  private final AtomicLong latestNetwork = new AtomicLong(-1);
  private final AtomicLong latestStatistics = new AtomicLong(-1);
  private static final Logger logger = LogManager.getLogger("time");

  public NetworkFirstStrategy(
      Scheduler scheduler,
      NetworkTime networkTime,
      StatisticsTime statisticsTime,
      int allowedDelta) {
    this.timeProcessor = new SimpleProcessor<Time>(scheduler, "NetworkFirstStrategy");
    Flux.from(networkTime.getTimeStream())
        .subscribe(
            t -> {
              this.latestNetwork.set(t.getValue());
              if (latestStatistics.get() == -1) {
                return;
              }
              if (Math.abs(latestNetwork.get() - latestStatistics.get()) <= allowedDelta) {
                timeProcessor.onNext(t);
              }
            });
    Flux.from(statisticsTime.getTimeStream())
        .subscribe(
            t -> {
              this.latestStatistics.set(t.getValue());
              if (latestNetwork.get() == -1) {
                return;
              }
              if (Math.abs(latestNetwork.get() - latestStatistics.get()) > allowedDelta) {
                logger.trace(() -> String.format("Using time from statistics time provider: %s", t.getValue()));
                timeProcessor.onNext(t);
              }
            });
  }

  @Override
  public Publisher<Time> getTimeStream() {
    return timeProcessor;
  }
}
