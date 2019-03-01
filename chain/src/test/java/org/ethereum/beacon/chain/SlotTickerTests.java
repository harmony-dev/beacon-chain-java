package org.ethereum.beacon.chain;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import org.junit.Test;
import reactor.core.publisher.Flux;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(chainSpec);
    genesisSlot = specHelpers.getChainSpec().getGenesisSlot();
    slotTicker = new SlotTicker(specHelpers, beaconState, Schedulers.createDefault());
  }

  @Test
  public void testSlotTicker() throws Exception {
    slotTicker.start();
    final CountDownLatch bothAssertsRun = new CountDownLatch(3);
    Flux.from(slotTicker.getTickerStream())
        .subscribe(
            slotNumber -> {
              if (previousTick.greater(SlotNumber.ZERO)) {
                assertEquals(previousTick.increment(), slotNumber);
                bothAssertsRun.countDown();
              } else {
                assertTrue(slotNumber.greater(genesisSlot)); // first tracked tick
                bothAssertsRun.countDown();
              }
              previousTick = slotNumber;
            });
    assertTrue(
        String.format("%s assertion(s) was not correct or not tested", bothAssertsRun.getCount()),
        bothAssertsRun.await(4, TimeUnit.SECONDS));
  }
}
