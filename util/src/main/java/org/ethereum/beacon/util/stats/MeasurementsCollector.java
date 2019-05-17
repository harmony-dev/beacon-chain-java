package org.ethereum.beacon.util.stats;

import java.util.ArrayList;
import java.util.List;

public class MeasurementsCollector extends TimeCollector {
  private List<Long> measurements = new ArrayList<>();

  @Override
  public void tick(long time) {
    super.tick(time);
    measurements.add(time);
  }

  public List<Long> getMeasurements() {
    return measurements;
  }
}
