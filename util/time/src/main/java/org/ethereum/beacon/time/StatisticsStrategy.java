package org.ethereum.beacon.time;

import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.time.provider.StatisticsTime;
import org.reactivestreams.Publisher;

public class StatisticsStrategy implements TimeStrategy {
  private final StatisticsTime statisticsTime;

  public StatisticsStrategy(StatisticsTime statisticsTime) {
    this.statisticsTime = statisticsTime;
  }

  @Override
  public Publisher<Time> getTimeStream() {
    return statisticsTime.getTimeStream();
  }
}
