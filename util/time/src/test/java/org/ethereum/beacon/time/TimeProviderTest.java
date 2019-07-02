package org.ethereum.beacon.time;

import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Schedulers;
import org.javatuples.Pair;
import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TimeProviderTest {

  @Ignore("10 seconds test")
  @Test
  public void testSystem() {
    TimeProvider provider =
        new SystemTime(
            Schedulers.createDefault().events(),
            Schedulers.createDefault().newSingleThreadDaemon("system-time"));
    standardTester(provider);
  }

  @Ignore("10 seconds test")
  @Test
  public void testNetwork() {
    Schedulers schedulers = Schedulers.createDefault();
    TimeProvider provider =
        new NetworkTime(
            schedulers.events(),
            schedulers.newSingleThreadDaemon("system-time"),
            schedulers.newSingleThreadDaemon("network-time"),
            5000);
    standardTester(provider);
  }

  @Test
  @Ignore("TODO: FIXME!!!!!")
  public void testStatistics() {
    Schedulers schedulers = Schedulers.createDefault();
    List<Publisher<Time>> objectTimeStreams = new ArrayList<>();
    TimeProvider provider1 =
        new NetworkTime(
            schedulers.events(),
            schedulers.newSingleThreadDaemon("system-time"),
            schedulers.newSingleThreadDaemon("network-time"),
            5000) {
          @Override
          long pullOffset(String serverHost) {
            return 1000;
          }
        };
    objectTimeStreams.add(provider1.getTimeStream());
    TimeProvider provider2 =
        new NetworkTime(
            schedulers.events(),
            schedulers.newSingleThreadDaemon("system-time"),
            schedulers.newSingleThreadDaemon("network-time"),
            5000) {
          @Override
          long pullOffset(String serverHost) {
            return 4000;
          }
        };
    objectTimeStreams.add(provider2.getTimeStream());
    TimeProvider provider3 =
        new NetworkTime(
            schedulers.events(),
            schedulers.newSingleThreadDaemon("system-time"),
            schedulers.newSingleThreadDaemon("network-time"),
            5000) {
          @Override
          long pullOffset(String serverHost) {
            return 5000;
          }
        };
    objectTimeStreams.add(provider3.getTimeStream());

    TimeProvider provider =
        new StatisticsTime(
            schedulers.events(),
            schedulers.newSingleThreadDaemon("system-time"),
            5,
            0,
            (Publisher<Time>[]) objectTimeStreams.toArray(new Publisher[0]));
    Pair<Long, List<Time>> res = gatherTimeObjects(provider);
    assertEquals(10, res.getValue1().size());
    for (int i = 0; i < 10; ++i) {
      assertEquals(res.getValue0() + i, res.getValue1().get(i).longValue());
    }
  }

  private void standardTester(TimeProvider provider) {
    Pair<Long, List<Time>> res = gatherTimeObjects(provider);
    assertEquals(10, res.getValue1().size());
    for (int i = 0; i < 10; ++i) {
      assertEquals(res.getValue0() + i, res.getValue1().get(i).longValue());
    }
  }

  private Pair<Long, List<Time>> gatherTimeObjects(TimeProvider provider) {
    // Move to XXX.050 second so we will not have any issues with start on .999 or .001
    long smartDelay = 1000 - Instant.now().getLong(ChronoField.MILLI_OF_SECOND) + 50;
    try {
      Thread.sleep(smartDelay);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    long start = Instant.now().getEpochSecond();
    List<Time> res = new ArrayList<>();
    Disposable subscription = Flux.from((provider.getTimeStream())).subscribe(res::add);

    // We are getting first one immediately, so need 9 more to get 10 in total
    while (Instant.now().getEpochSecond() < (start + 9)) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    subscription.dispose();

    return Pair.with(start, res);
  }
}
