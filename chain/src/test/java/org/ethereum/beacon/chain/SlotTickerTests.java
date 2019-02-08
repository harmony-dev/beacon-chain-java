package org.ethereum.beacon.chain;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.junit.Test;
import reactor.core.publisher.Flux;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SlotTickerTests {
  public static final int MILLIS_IN_SECOND = 1000;
  SlotTicker slotTicker;
  long genesisSlot;

  public SlotTickerTests() throws InterruptedException {
    MutableBeaconState beaconState = BeaconState.getEmpty().createMutableCopy();
    while (System.currentTimeMillis() % MILLIS_IN_SECOND < 100
        || System.currentTimeMillis() % MILLIS_IN_SECOND > 900) {
      Thread.sleep(100);
    }
    beaconState.setGenesisTime(
        Time.of(System.currentTimeMillis() / MILLIS_IN_SECOND).minus(Time.of(2)));
    ChainSpec chainSpec =
        new ChainSpec() {
          @Override
          public SlotNumber getGenesisSlot() {
            return SlotNumber.of(12345);
          }

          @Override
          public Time getSlotDuration() {
            return Time.of(1);
          }
        };
    SpecHelpers specHelpers = new SpecHelpers(chainSpec);
    genesisSlot = specHelpers.getChainSpec().getGenesisSlot().longValue();
    slotTicker = new SlotTicker(specHelpers, beaconState);
  }

  @Test
  public void testSlotTicker() throws Exception {
    slotTicker.start();
    Thread.sleep(2000);
    final AtomicBoolean first = new AtomicBoolean(true);
    final CountDownLatch bothAssertsRun = new CountDownLatch(2);
    Flux.from(slotTicker.getTickerStream())
        .subscribe(
            slotNumber -> {
              if (first.get()) {
                assertEquals(SlotNumber.of(genesisSlot + 4), slotNumber);
                bothAssertsRun.countDown();
                first.set(false);
              } else { // second
                assertEquals(SlotNumber.of(genesisSlot + 5), slotNumber);
                bothAssertsRun.countDown();
              }
            });
    assertTrue(
        String.format("%s assertion(s) was not correct or not tested", bothAssertsRun.getCount()),
        bothAssertsRun.await(2, TimeUnit.SECONDS));
  }
}
