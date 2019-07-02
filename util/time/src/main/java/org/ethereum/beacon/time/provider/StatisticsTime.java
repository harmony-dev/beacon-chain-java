package org.ethereum.beacon.time.provider;

import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Time provider uses statistics from ethereum network objects
 *
 * <p>Keeps statistics for latest secondsBuffer seconds, calculates offsets to systemTime on this
 * sample of data, uses {@link #offsetAggregator(Map)} to get the best single offset value and adds
 * it to system time to provide output value
 *
 * <p>See <a
 * href="https://ethresear.ch/t/network-adjusted-timestamps/4187">https://ethresear.ch/t/network-adjusted-timestamps/4187</a>
 */
public class StatisticsTime implements TimeProvider {
  private final SimpleProcessor<Time> timeProcessor;
  private final Map<Long, Map<Long, AtomicInteger>> data = new HashMap<>();
  private final AtomicLong latestSystem = new AtomicLong(-1);
  private final AtomicInteger offset = new AtomicInteger(0);

  @SafeVarargs
  public StatisticsTime(
      Scheduler events,
      Scheduler worker,
      int secondsBuffer,
      int filterOutliers,
      Publisher<Time>... objectTimeStreams) {
    this.timeProcessor = new SimpleProcessor<Time>(events, "TimeProvider.system");
    SystemTime systemTime = new SystemTime(events, worker);
    Flux.from(systemTime.getTimeStream())
        .subscribe(
            time -> {
              Map<Integer, Integer> offsets = gatherDataIntoOffsets(filterOutliers);
              long newOffset = offsetAggregator(offsets);
              offset.set((int) newOffset);
              data.remove(time.getValue() - secondsBuffer);
              latestSystem.set(time.getValue());
              timeProcessor.onNext(Time.of(time.getValue() + newOffset));
              data.put(time.getValue(), new HashMap<>());
            });
    Flux.concat(objectTimeStreams)
        .subscribe(
            time -> {
              Map<Long, AtomicInteger> latestStat = data.get(latestSystem.get());
              if (latestStat.containsKey(time.getValue())) {
                latestStat.get(time.getValue()).incrementAndGet();
              } else {
                latestStat.put(time.getValue(), new AtomicInteger(1));
              }
            });
  }

  private Map<Integer, Integer> gatherDataIntoOffsets(int filterOutliers) {
    return data.entrySet().stream()
        // => stream(offset: counter)
        .flatMap(
            (Function<Map.Entry<Long, Map<Long, AtomicInteger>>, Stream<Pair<Integer, Integer>>>)
                longMapEntry -> {
                  long systemTime1 = longMapEntry.getKey();
                  List<Pair<Integer, Integer>> offsetCounters = new ArrayList<>();
                  for (Map.Entry<Long, AtomicInteger> entry : longMapEntry.getValue().entrySet()) {
                    offsetCounters.add(
                        Pair.with((int) (systemTime1 - entry.getKey()), entry.getValue().get()));
                  }
                  return offsetCounters.stream();
                })
        .collect( // offset -> sum of counters
            Collectors.groupingBy(Pair::getValue0, Collectors.summarizingInt(Pair::getValue1)))
        .entrySet()
        .stream()
        // filter outliers
        .filter(e -> e.getValue().getSum() > filterOutliers)
        .map(e -> Pair.with(e.getKey(), Math.toIntExact(e.getValue().getSum())))
        .collect(Collectors.toMap(Pair::getValue0, Pair::getValue1));
  }

  /**
   * Taking into account offset statistics: [offset: count] provides estimated single number offset
   * with such kind of input by applying some statistic aggregation function (depends on
   * implementation)
   */
  Integer offsetAggregator(Map<Integer, Integer> data) {
    // Median calculation
    List<Integer> offsets = new ArrayList<>(data.keySet());
    if (offsets.isEmpty()) {
      return 0;
    }
    offsets.sort(Integer::compareTo);
    int middle = offsets.size() / 2;
    if (offsets.size() % 2 == 0) {
      return (offsets.get(middle) + offsets.get(middle - 1)) / 2;
    } else {
      return offsets.get(middle);
    }
  }

  @Override
  public Publisher<Time> getTimeStream() {
    return timeProcessor;
  }
}
