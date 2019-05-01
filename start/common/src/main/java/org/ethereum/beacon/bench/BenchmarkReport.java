package org.ethereum.beacon.bench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.ethereum.beacon.bench.BenchmarkController.BenchmarkRoutine;
import org.ethereum.beacon.util.stats.MeasurementsCollector;
import org.ethereum.beacon.util.stats.TimeCollector;

public class BenchmarkReport {

  public static final double PERCENTILE_RATIO = 0.95;

  private List<RoutineReport> routines;

  public String print() {
    StringBuilder sb = new StringBuilder();
    routines.forEach(routine -> sb.append(routine.print()).append("\n\n\n"));
    return sb.delete(sb.length() - 3, sb.length()).toString();
  }

  enum MeasurementGroup {
    TOP_METHODS,
    BLS,
    HELPERS;

    public String print() {
      return this.name().replaceAll("_", " ");
    }
  }

  public static class RoutineReport {
    private BenchmarkRoutine routine;
    private List<GroupReport> groups;
    private RoutineSummary summary;

    public RoutineReport(
        BenchmarkRoutine routine, List<GroupReport> groups, RoutineSummary summary) {
      this.routine = routine;
      this.groups = groups;
      this.summary = summary;
    }

    public String print() {
      StringBuilder sb = new StringBuilder();
      sb.append(routine.print()).append("\n");
      groups.forEach(group -> sb.append(group.print("  ")).append("\n\n"));
      sb.append(summary.print("  "));
      return sb.toString();
    }
  }

  public static class GroupReport {
    private MeasurementGroup group;
    private List<FunctionStats> functions;

    public GroupReport(MeasurementGroup group, List<FunctionStats> functions) {
      this.group = group;
      this.functions = functions;
    }

    public String print(String leftPadding) {
      StringBuilder sb = new StringBuilder();
      sb.append(
              FunctionStats.format(
                  leftPadding + group.print(),
                  "min, ms",
                  "avg, ms",
                  "95%, ms",
                  "count",
                  "total, ms"))
          .append('\n');
      functions.forEach(stats -> sb.append(stats.print(leftPadding + leftPadding)).append('\n'));
      return sb.deleteCharAt(sb.length() - 1).toString();
    }
  }

  public static class RoutineSummary {
    private SummaryStats topMethodsSummary;
    private List<GroupSummary> groupSummaries;

    public RoutineSummary(List<GroupSummary> groupSummaries) {
      this.groupSummaries = groupSummaries;
    }

    public String print(String leftPadding) {
      StringBuilder sb = new StringBuilder();
      sb.append(SummaryStats.format(leftPadding + "SUMMARY", "min, ms", "avg, ms", "% of total"))
          .append('\n');
      sb.append(topMethodsSummary.print(leftPadding + leftPadding)).append('\n');
      groupSummaries.forEach(
          groupSummary -> sb.append(groupSummary.print(leftPadding)).append('\n'));
      return sb.deleteCharAt(sb.length() - 1).toString();
    }
  }

  public static class GroupSummary {
    private SummaryStats groupSummary;
    private List<SummaryStats> functionSummaries;

    public GroupSummary(SummaryStats groupSummary, List<SummaryStats> functionSummaries) {
      this.groupSummary = groupSummary;
      this.functionSummaries = functionSummaries;
    }

    public String print(String leftPadding) {
      StringBuilder sb = new StringBuilder();
      sb.append(groupSummary.printTitleOnly(leftPadding + leftPadding)).append('\n');
      functionSummaries.forEach(
          functionSummary ->
              sb.append(functionSummary.print(leftPadding + leftPadding + leftPadding))
                  .append('\n'));
      return sb.deleteCharAt(sb.length() - 1).toString();
    }
  }

  public static class SummaryStats {
    private String title;
    private long minTime = 0;
    private double avgTime = 0;
    private double ratioToTotal = 0;

    public SummaryStats(String title) {
      this.title = title;
    }

    static String format(String title, String minTime, String avgTime, String percentage) {
      return String.format("%-45s%15s%15s%15s", title, minTime, avgTime, percentage);
    }

    public String print(String leftPadding) {
      return format(
          leftPadding + title,
          String.format("%.3f", minTime / 1_000_000d),
          String.format("%.3f", avgTime / 1_000_000d),
          String.format("%.2f%%", ratioToTotal * 100));
    }

    public String printTitleOnly(String leftPadding) {
      return format(leftPadding + title, "", "", "");
    }
  }

  public static class FunctionStats {
    private String name;
    private long minTime = 0;
    private double avgTime = 0;
    private long percentile = 0;
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

    public long getPercentile() {
      return percentile;
    }

    public int getCounter() {
      return counter;
    }

    static String format(
        String title,
        String minTime,
        String avgTime,
        String percentile,
        String counter,
        String totalTime) {
      return String.format(
          "%-45s%15s%15s%15s%15s%15s", title, minTime, avgTime, percentile, counter, totalTime);
    }

    public String print(String leftPadding) {
      return format(
          leftPadding + name,
          String.format("%.3f", minTime / 1_000_000d / counter),
          String.format("%.3f", avgTime / 1_000_000d / counter),
          String.format("%.3f", percentile / 1_000_000d),
          String.valueOf(counter),
          String.format("%.3f", avgTime / 1_000_000d));
    }
  }

  public static class Builder {

    private final Map<BenchmarkRoutine, List<Map<String, MeasurementsCollector>>> measuredRoutines =
        new HashMap<>();

    public Builder addRoutine(
        BenchmarkRoutine routine, List<Map<String, MeasurementsCollector>> measurements) {
      measuredRoutines.put(routine, measurements);
      return this;
    }

    public BenchmarkReport build() {
      BenchmarkReport report = new BenchmarkReport();
      report.routines = new ArrayList<>();

      for (BenchmarkRoutine routine : BenchmarkRoutine.values()) {
        List<Map<String, MeasurementsCollector>> measurements = measuredRoutines.get(routine);
        if (measurements == null) continue;

        List<GroupReport> groupReports = new ArrayList<>();
        List<GroupSummary> groupSummaries = new ArrayList<>();
        RoutineSummary routineSummary = new RoutineSummary(groupSummaries);
        report.routines.add(new RoutineReport(routine, groupReports, routineSummary));

        GroupReport topMethodsReport = null;
        for (MeasurementGroup group : MeasurementGroup.values()) {
          String[] functionList = getFunctionList(routine, group);
          List<FunctionStats> functionStats =
              Arrays.stream(functionList)
                  .map(
                      name -> {
                        // collection stats per function
                        List<MeasurementsCollector> functionMeasurements =
                            measurements.stream()
                                .map(
                                    checkpoint ->
                                        checkpoint.getOrDefault(name, new MeasurementsCollector()))
                                .collect(Collectors.toList());
                        return getFunctionStats(name, functionMeasurements);
                      })
                  // filter out functions that hasn't been called
                  .filter(function -> function.counter > 0)
                  // sort by avg time descending
                  .sorted((o1, o2) -> Double.compare(o2.avgTime, o1.avgTime))
                  .collect(Collectors.toList());

          if (!functionStats.isEmpty()) {
            GroupReport groupReport = new GroupReport(group, functionStats);
            groupReports.add(groupReport);
            if (group == MeasurementGroup.TOP_METHODS) {
              topMethodsReport = groupReport;
            }
          }
        }

        if (topMethodsReport != null) {
          double highLevelGroupTime =
              topMethodsReport.functions.stream().mapToDouble(FunctionStats::getAvgTime).sum();
          if (highLevelGroupTime > 0) {
            for (GroupReport groupReport : groupReports) {
              if (groupReport == topMethodsReport) {
                routineSummary.topMethodsSummary =
                    getGroupSummaryStats(
                        groupReport.group.print(), groupReport, highLevelGroupTime);
              } else {
                groupSummaries.add(getGroupSummary(groupReport, highLevelGroupTime));
              }
            }
            Collections.sort(
                groupSummaries,
                (o1, o2) -> Double.compare(o2.groupSummary.avgTime, o1.groupSummary.avgTime));
          }
        }
      }

      return report;
    }

    private GroupSummary getGroupSummary(GroupReport groupReport, double highLevelGroupTime) {
      SummaryStats groupSummaryStats =
          getGroupSummaryStats(groupReport.group.name(), groupReport, highLevelGroupTime);
      List<SummaryStats> functionSummaries =
          groupReport.functions.stream()
              .map(
                  functionStats -> {
                    SummaryStats summary = new SummaryStats(functionStats.name);
                    summary.minTime = functionStats.minTime;
                    summary.avgTime = functionStats.avgTime;
                    summary.ratioToTotal = functionStats.avgTime / highLevelGroupTime;
                    return summary;
                  })
              .collect(Collectors.toList());

      return new GroupSummary(groupSummaryStats, functionSummaries);
    }

    private SummaryStats getGroupSummaryStats(
        String title, GroupReport groupReport, double highLevelGroupTime) {
      SummaryStats groupStats = new SummaryStats(title);
      groupStats.minTime =
          groupReport.functions.stream().mapToLong(FunctionStats::getMinTime).sum();
      groupStats.avgTime =
          groupReport.functions.stream().mapToDouble(FunctionStats::getAvgTime).sum();
      groupStats.ratioToTotal = groupStats.avgTime / highLevelGroupTime;
      return groupStats;
    }

    private FunctionStats getFunctionStats(String name, List<MeasurementsCollector> measurements) {
      FunctionStats stats = new FunctionStats(name);
      List<Long> measurementChurn =
          measurements.stream()
              .flatMap(m -> m.getMeasurements().stream())
              .collect(Collectors.toList());
      stats.counter = measurements.stream().mapToInt(TimeCollector::getCounter).max().orElse(0);
      stats.minTime = measurementChurn.stream().min(Long::compareTo).orElse(0L);
      stats.avgTime = measurements.stream().mapToLong(TimeCollector::getTotal).average().orElse(0);
      stats.percentile = BenchmarkUtils.percentile(PERCENTILE_RATIO, measurementChurn);
      return stats;
    }

    private String[] getFunctionList(BenchmarkRoutine routine, MeasurementGroup group) {
      switch (group) {
        case BLS:
          return BLS_FUNCTIONS;
        case HELPERS:
          return HELPER_FUNCTIONS;
        case TOP_METHODS:
          if (!TOP_METHOD_LIST.containsKey(routine)) {
            throw new IllegalArgumentException("Unsupported benchmark routine: " + routine);
          } else {
            return TOP_METHOD_LIST.get(routine);
          }
        default:
          throw new IllegalArgumentException("Unsupported measurement group: " + group);
      }
    }
  }

  private static final Map<BenchmarkRoutine, String[]> TOP_METHOD_LIST = new HashMap<>();

  static {
    TOP_METHOD_LIST.put(BenchmarkRoutine.SLOT, new String[] {"cache_state", "advance_slot"});
    TOP_METHOD_LIST.put(
        BenchmarkRoutine.BLOCK,
        new String[] {
          "process_block_header", "process_randao", "process_eth1_data", "process_attestation"
        });
    TOP_METHOD_LIST.put(
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

  private static final String[] HELPER_FUNCTIONS = {
    "hash_tree_root",
    "signing_root",
    "get_crosslink_committees_at_slot",
    "get_beacon_proposer_index",
    "get_active_validator_indices",
    "get_total_balance",
    "get_attesting_indices",
    "get_validator_index_by_pubkey",
    "get_previous_total_balance",
    "get_total_active_balance",
    "get_base_reward",
    "verify_bitfield"
  };
}
