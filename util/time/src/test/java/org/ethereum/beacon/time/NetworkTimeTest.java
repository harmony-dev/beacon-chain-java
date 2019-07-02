package org.ethereum.beacon.time;

import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Schedulers;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class NetworkTimeTest {
  @Ignore("10 seconds test")
  @Test
  public void testNegativeOffset() {
    Schedulers schedulers = Schedulers.createDefault();
    TimeProvider provider =
        new NetworkTime(
            schedulers.events(),
            schedulers.newSingleThreadDaemon("system-time"),
            schedulers.newSingleThreadDaemon("network-time"),
            5000) {
          private int currentQuery = 0;

          @Override
          long pullOffset(String serverHost) {
            long ret;
            if (currentQuery == 0) {
              ret = -500; // moved to previous second
            } else {
              ret = -1500; // 1500 total, so only one time is delayed
            }

            ++currentQuery;
            return ret;
          }
        };

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

    // We are getting first one immediately, so need 9 more to get 9 in total
    while (Instant.now().getEpochSecond() < (start + 9)) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    subscription.dispose();

    assertEquals(9, res.size());
    for (int i = 0; i < 9; ++i) {
      assertEquals(start + i - 1, res.get(i).longValue());
    }
  }

  @Ignore("10 seconds test")
  @Test
  public void testBigNegativeOffset() {
    Schedulers schedulers = Schedulers.createDefault();
    TimeProvider provider =
        new NetworkTime(
            schedulers.events(),
            schedulers.newSingleThreadDaemon("system-time"),
            schedulers.newSingleThreadDaemon("network-time"),
            5000) {
          @Override
          long pullOffset(String serverHost) {
            return -4500;
          }
        };

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

    // We are getting first one immediately, so we are getting 9 in total after 9 seconds
    while (Instant.now().getEpochSecond() < (start + 9)) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    subscription.dispose();

    assertEquals(10, res.size());
    for (int i = 0; i < 10; ++i) {
      assertEquals(start + i - 5, res.get(i).longValue());
    }
  }

  @Ignore("10 seconds test")
  @Test
  public void testNegativePositiveOffset() {
    Schedulers schedulers = Schedulers.createDefault();
    TimeProvider provider =
        new NetworkTime(
            schedulers.events(),
            schedulers.newSingleThreadDaemon("system-time"),
            schedulers.newSingleThreadDaemon("network-time"),
            5000) {
          private int currentQuery = 0;

          @Override
          long pullOffset(String serverHost) {
            long ret;
            if (currentQuery == 0) {
              ret = 1500; // effective immediately
            } else {
              ret = -1500; // so total -3 seconds backwards after the middle
            }

            ++currentQuery;
            return ret;
          }
        };

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

    assertEquals(10, res.size());
    for (int i = 0; i < 6; ++i) {
      assertEquals(start + i + 1, res.get(i).longValue());
    }
    for (int i = 6; i < 10; ++i) { // + 1 -> - 2, 3 seconds difference
      assertEquals(start + i - 2, res.get(i).longValue());
    }
  }
}
