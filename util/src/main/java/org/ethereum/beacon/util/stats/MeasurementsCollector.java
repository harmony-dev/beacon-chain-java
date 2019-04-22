package org.ethereum.beacon.util.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

  public long percentile(double threshold) {
    if (measurements.isEmpty()) {
      return 0;
    }

    List<Long> sortedMeasurements = measurements.stream().sorted().collect(Collectors.toList());
    int index = (int) Math.max(0, Math.floor(threshold * measurements.size()) - 1);
    return sortedMeasurements.get(index);
  }
}
