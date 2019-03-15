package org.ethereum.beacon.util.stats;

public class TimeCollector {
  private int counter = 0;
  private long total = 0;

  public void tick(long time) {
    total += time;
    counter += 1;
  }

  public void reset() {
    total = counter = 0;
  }

  public long getAvg() {
    return total / counter;
  }
}
