package org.ethereum.beacon.bench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.ethereum.beacon.bench.BenchmarkController.BenchmarkRoutine;
import org.ethereum.beacon.util.stats.TimeCollector;

public class BenchmarkReport {

  private List<RoutineReport> routines;

  public String print() {
    StringBuilder sb = new StringBuilder();
    routines.forEach(routine -> sb.append(routine.print()).append("\n\n\n"));
    return sb.delete(sb.length() - 3, sb.length()).toString();
  }

  enum MeasurementGroup {
    HIGH_LEVEL,
    BLS,
    HELPERS,
    SUMMARY;

    public String print() {
      return this.name().replaceAll("_", " ");
    }
  }

  public static class RoutineReport {
    private BenchmarkRoutine routine;
    private List<GroupReport> groups;

    public RoutineReport(BenchmarkRoutine routine, List<GroupReport> groups) {
      this.routine = routine;
      this.groups = groups;
    }

    public String print() {
      StringBuilder sb = new StringBuilder();
      sb.append(routine.print()).append(":\n\n");
      groups.forEach(group -> sb.append(group.print()).append("\n\n"));
      return sb.delete(sb.length() - 2, sb.length()).toString();
    }
  }

  public static class GroupReport {
    private MeasurementGroup group;
    private List<FunctionStats> functions;

    public GroupReport(MeasurementGroup group, List<FunctionStats> functions) {
      this.group = group;
      this.functions = functions;
    }

    public String print() {
      StringBuilder sb = new StringBuilder();
      sb.append(FunctionStats.format("[" + group.print() + "]", "min, ms", "avg, ms", "count"))
          .append('\n');
      functions.forEach(stats -> sb.append(stats.print()).append('\n'));
      return sb.deleteCharAt(sb.length() - 1).toString();
    }
  }

  public static class FunctionStats {
    private String name;
    private long minTime = 0;
    private double avgTime = 0;
    private long percentile95Time = 0;
    private int counter = 0;

    public FunctionStats(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public long getMinTime() {
      return minTime;
    }

    public double getAvgTime() {
      return avgTime;
    }

    public long getPercentile95Time() {
      return percentile95Time;
    }

    public int getCounter() {
      return counter;
    }

    static String format(String name, String minTime, String avgTime, String counter) {
      return String.format("%-40s%15s%15s%10s", name, minTime, avgTime, counter);
    }

    public String print() {
      return format(
          name,
          String.format("%.3f", minTime / 1_000_000d / counter),
          String.format("%.3f", avgTime / 1_000_000d / counter),
          String.valueOf(counter));
    }
  }

  public static class Builder {

    private final Map<BenchmarkRoutine, List<Map<String, TimeCollector>>> measuredRoutines =
        new HashMap<>();

    public Builder addRoutine(
        BenchmarkRoutine routine, List<Map<String, TimeCollector>> measurements) {
      measuredRoutines.put(routine, measurements);
      return this;
    }

    public BenchmarkReport build() {
      BenchmarkReport report = new BenchmarkReport();
      report.routines = new ArrayList<>();

      for (BenchmarkRoutine routine : BenchmarkRoutine.values()) {
        List<Map<String, TimeCollector>> measurements = measuredRoutines.get(routine);
        if (measurements == null) continue;

        List<GroupReport> groupReports = new ArrayList<>();
        report.routines.add(new RoutineReport(routine, groupReports));
        for (MeasurementGroup group : MeasurementGroup.values()) {
          String[] functionList = getFunctionList(routine, group);
          List<FunctionStats> functionStats =
              Arrays.stream(functionList)
                  .map(
                      name -> {
                        // collection stats per function
                        List<TimeCollector> functionMeasurements =
                            measurements.stream()
                                .map(
                                    checkpoint ->
                                        checkpoint.getOrDefault(name, new TimeCollector()))
                                .collect(Collectors.toList());
                        return getFunctionStats(name, functionMeasurements);
                      })
                  // filter out functions that hasn't been called
                  .filter(function -> function.counter > 0)
                  // sort by avg time descending
                  .sorted((o1, o2) -> -1 * Double.compare(o1.avgTime, o2.avgTime))
                  .collect(Collectors.toList());

          if (!functionStats.isEmpty()) {
            // special case for MeasurementGroup.HIGH_LEVEL: add stats for the whole flow
            if (group == MeasurementGroup.HIGH_LEVEL) {
              FunctionStats stats = new FunctionStats("total");
              stats.counter = 1;
              stats.minTime = functionStats.stream().mapToLong(FunctionStats::getMinTime).sum();
              stats.avgTime = functionStats.stream().mapToDouble(FunctionStats::getAvgTime).sum();
              functionStats.add(stats);
            }
            groupReports.add(new GroupReport(group, functionStats));
          }
        }
      }

      return report;
    }

    private FunctionStats getFunctionStats(String name, List<TimeCollector> measurements) {
      FunctionStats stats = new FunctionStats(name);
      stats.counter = measurements.stream().mapToInt(TimeCollector::getCounter).max().orElse(0);
      stats.minTime =
          measurements.stream()
              .mapToLong(TimeCollector::getTotal)
              //              .filter(value -> value > 0)
              .min()
              .orElse(0);
      stats.avgTime = measurements.stream().mapToLong(TimeCollector::getTotal).average().orElse(0);
      return stats;
    }

    private String[] getFunctionList(BenchmarkRoutine routine, MeasurementGroup group) {
      switch (group) {
        case BLS:
          return BLS_FUNCTIONS;
        case HELPERS:
          return HELPER_FUNCTIONS;
        case HIGH_LEVEL:
          if (!HIGH_LEVEL_FUNCTIONS.containsKey(routine)) {
            throw new IllegalArgumentException("Unsupported benchmark routine: " + routine);
          } else {
            return HIGH_LEVEL_FUNCTIONS.get(routine);
          }
        case SUMMARY:
          return new String[0];
        default:
          throw new IllegalArgumentException("Unsupported measurement group: " + group);
      }
    }
  }

  private static final Map<BenchmarkRoutine, String[]> HIGH_LEVEL_FUNCTIONS = new HashMap<>();

  static {
    HIGH_LEVEL_FUNCTIONS.put(BenchmarkRoutine.SLOT, new String[] {"cache_state", "advance_slot"});
    HIGH_LEVEL_FUNCTIONS.put(BenchmarkRoutine.BLOCK, new String[] {"process_attestation"});
    HIGH_LEVEL_FUNCTIONS.put(
        BenchmarkRoutine.EPOCH,
        new String[] {
          "update_justification_and_finalization",
          "process_crosslinks",
          "maybe_reset_eth1_period",
          "apply_rewards",
          "process_ejections",
          "update_registry_and_shuffling_data",
          "process_slashings",
          "process_exit_queue",
          "finish_epoch_update"
        });
  }

  private static final String[] BLS_FUNCTIONS = {
    "bls_aggregate_pubkeys", "bls_verify_multiple", "bls_verify"
  };

  private static final String[] HELPER_FUNCTIONS = {"hash_tree_root", "signed_root"};
}
