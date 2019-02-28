package org.ethereum.beacon.chain.observer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.ethereum.beacon.chain.util.SampleObservableState;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;

public class ObservableStateProcessorTest {

  @Test
  public void test1() throws Exception {
    ControlledSchedulers schedulers = Schedulers.createControlled();
    Duration genesisTime = Duration.ofMinutes(10);
    SlotNumber genesisSlot = SlotNumber.of(1_000_000);
    schedulers.setCurrentTime((genesisTime.getSeconds() + 1) * 1000);

    SampleObservableState sample = new SampleObservableState(new Random(1),
        genesisTime, genesisSlot.getValue(), Duration.ofSeconds(10), 8, s -> {
    }, schedulers);

    List<ObservableBeaconState> states = new ArrayList<>();
    Flux.from(sample.observableStateProcessor.getObservableStateStream()).subscribe(states::add);

    List<SlotNumber> slots = new ArrayList<>();
    Flux.from(sample.slotTicker.getTickerStream()).subscribe(slots::add);

    System.out.println(states);
    System.out.println(slots);

    Assert.assertEquals(0, states.size());
    Assert.assertEquals(0, slots.size());

    schedulers.addTime(Duration.ofSeconds(10));

    System.out.println(states);
    System.out.println(slots);

    Assert.assertEquals(1, states.size());
    Assert.assertEquals(1, slots.size());

    Assert.assertEquals(genesisSlot.getValue() + 1, slots.get(0).getValue());
    Assert.assertEquals(genesisSlot.increment(), states.get(0).getLatestSlotState().getSlot());
  }

  @Test
  public void test2() throws Exception {
    ControlledSchedulers schedulers = Schedulers.createControlled();
    Duration genesisTime = Duration.ofMinutes(10);
    SlotNumber genesisSlot = SlotNumber.of(1_000_000);
    schedulers.setCurrentTime(genesisTime.plus(Duration.ofMinutes(10)).toMillis());

    SampleObservableState sample = new SampleObservableState(new Random(1),
        genesisTime, genesisSlot.getValue(), Duration.ofSeconds(10), 8, s -> {
    }, schedulers);

    List<ObservableBeaconState> states = new ArrayList<>();
    Flux.from(sample.observableStateProcessor.getObservableStateStream()).subscribe(s -> states.add(s));

    List<SlotNumber> slots = new ArrayList<>();
    Flux.from(sample.slotTicker.getTickerStream()).subscribe(slots::add);

    Assert.assertEquals(0, states.size());
    Assert.assertEquals(0, slots.size());

    schedulers.addTime(Duration.ofSeconds(10));

    Assert.assertEquals(1, states.size());
    Assert.assertEquals(1, slots.size());

    Assert.assertEquals(genesisSlot.getValue() + 61, slots.get(0).getValue());
    Assert.assertEquals(genesisSlot.plus(61), states.get(0).getLatestSlotState().getSlot());
  }
}
