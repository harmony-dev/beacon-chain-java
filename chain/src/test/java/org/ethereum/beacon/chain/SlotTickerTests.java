package org.ethereum.beacon.chain;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.*;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.*;
import org.ethereum.beacon.schedulers.Schedulers;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Flux;

import java.util.concurrent.*;

public class SlotTickerTests {
  public static final int MILLIS_IN_SECOND = 1000;
  private final Schedulers schedulers;
  SlotTicker slotTicker;
  SlotNumber genesisSlot;
  SlotNumber previousTick = SlotNumber.ZERO;

  public SlotTickerTests() throws InterruptedException {
    schedulers = Schedulers.createDefault();
    MutableBeaconState beaconState = BeaconState.getEmpty().createMutableCopy();
    while (schedulers.getCurrentTime() % MILLIS_IN_SECOND < 100
        || schedulers.getCurrentTime() % MILLIS_IN_SECOND > 900) {
      Thread.sleep(100);
    }
    beaconState.setGenesisTime(
        Time.of(schedulers.getCurrentTime() / MILLIS_IN_SECOND).minus(Time.of(2)));
    SpecConstants specConstants =
        new SpecConstants() {
          @Override
          public SlotNumber getGenesisSlot() {
            return SlotNumber.of(12345);
          }

          @Override
          public Time getSecondsPerSlot() {
            return Time.of(1);
          }
        };
    BeaconChainSpec spec = BeaconChainSpec.createWithDefaultHasher(specConstants);
    genesisSlot = spec.getConstants().getGenesisSlot();
    slotTicker = new SlotTicker(spec, beaconState, Schedulers.createDefault());
  }

  @Test
  public void testSlotTicker() throws Exception {
    slotTicker.start();
    final CountDownLatch bothAssertsRun = new CountDownLatch(3);
    Flux.from(slotTicker.getTickerStream())
        .subscribe(
            slotNumber -> {
              if (previousTick.greater(SlotNumber.ZERO)) {
                  Assertions.assertEquals(previousTick.increment(), slotNumber);
                bothAssertsRun.countDown();
              } else {
                  Assertions.assertTrue(slotNumber.greater(genesisSlot)); // first tracked tick
                bothAssertsRun.countDown();
              }
              previousTick = slotNumber;
            });
      Assertions.assertTrue(
              (bothAssertsRun.await(4, TimeUnit.SECONDS)),
              String.format("%s assertion(s) was not correct or not tested", bothAssertsRun.getCount()));
  }
}
