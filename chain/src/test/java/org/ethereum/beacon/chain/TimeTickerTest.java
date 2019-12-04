package org.ethereum.beacon.chain;

import java.util.concurrent.atomic.AtomicLong;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;

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
    Assert.assertEquals(1000, timeHolder.get());

    schedulers.addTime(2000);
    Assert.assertEquals(3000, timeHolder.get());
  }
}
