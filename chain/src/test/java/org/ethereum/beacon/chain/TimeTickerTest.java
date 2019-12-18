package org.ethereum.beacon.chain;

import java.util.concurrent.atomic.AtomicLong;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.uint.UInt64;

public class TimeTickerTest {

  @Test
  public void checkWithControlledSchedulers() {
    ControlledSchedulers schedulers = Schedulers.createControlled();
    schedulers.setCurrentTime(0);

    TimeTicker timeTicker = new TimeTicker(schedulers);
    AtomicLong timeHolder = new AtomicLong(0);
    Flux.from(timeTicker.getTickerStream()).subscribe(t -> timeHolder.set(t.getValue()));
    timeTicker.start();

    schedulers.addTime(500);

    Assert.assertEquals(0, timeHolder.get());

    schedulers.addTime(500);
    Assert.assertEquals(1, timeHolder.get());

    schedulers.addTime(2000);
    Assert.assertEquals(3, timeHolder.get());
  }

  @Test
  public void checkIfSlotStreamWorks() {
    Time genesisTime = Time.of(1);
    Time secondsPerSlot = Time.of(12);

    ControlledSchedulers schedulers = Schedulers.createControlled();
    schedulers.setCurrentTime(0);

    TimeTicker timeTicker = new TimeTicker(schedulers);
    timeTicker.start();

    Flux<SlotNumber> slotStream =
        Flux.from(timeTicker.getTickerStream())
            .filter(time -> time.greaterEqual(genesisTime))
            .filter(time -> time.minus(genesisTime).modulo(secondsPerSlot).equals(UInt64.ZERO))
            .map(time -> SlotNumber.castFrom(time.minus(genesisTime).dividedBy(secondsPerSlot)))
            .subscribeOn(schedulers.events().toReactor());

    AtomicLong slotHolder = new AtomicLong(-1);
    Flux.from(slotStream).subscribe(slot -> slotHolder.set(slot.getValue()));

    schedulers.addTime(500);
    Assert.assertEquals(-1, slotHolder.get());

    schedulers.addTime(500);
    Assert.assertEquals(0, slotHolder.get());

    schedulers.addTime(2000);
    Assert.assertEquals(0, slotHolder.get());

    schedulers.setCurrentTime(genesisTime.plus(secondsPerSlot).getMillis().getValue());
    Assert.assertEquals(1, slotHolder.get());

    schedulers.setCurrentTime(genesisTime.plus(secondsPerSlot.times(2)).getMillis().getValue());
    Assert.assertEquals(2, slotHolder.get());
  }
}
