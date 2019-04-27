package org.ethereum.beacon.bench;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BenchmarkUtils {
  private BenchmarkUtils() {}

  public static long percentile(double ratio, List<Long> measurements) {
    if (measurements.isEmpty()) {
      return 0;
    }

    List<Long> sortedMeasurements = measurements.stream().sorted().collect(Collectors.toList());
    int index = (int) Math.max(0, Math.floor(ratio * sortedMeasurements.size()) - 1);
    return measurements.get(index);
  }
}
