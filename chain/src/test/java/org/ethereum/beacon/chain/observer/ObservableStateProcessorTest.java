package org.ethereum.beacon.chain.observer;

import org.ethereum.beacon.chain.util.SampleObservableState;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.*;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;

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

    Assertions.assertEquals(1, states.size());
    Assertions.assertEquals(0, slots.size());

    schedulers.addTime(Duration.ofSeconds(10));

    System.out.println(states);
    System.out.println(slots);

    Assertions.assertEquals(2, states.size());
    Assertions.assertEquals(1, slots.size());

    Assertions.assertEquals(genesisSlot.getValue() + 1, slots.get(0).getValue());
    Assertions.assertEquals(genesisSlot.increment(), states.get(1).getLatestSlotState().getSlot());
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

    Assertions.assertEquals(1, states.size());
    Assertions.assertEquals(0, slots.size());

    schedulers.addTime(Duration.ofSeconds(10));

    Assertions.assertEquals(2, states.size());
    Assertions.assertEquals(1, slots.size());

    Assertions.assertEquals(genesisSlot.getValue() + 61, slots.get(0).getValue());
    Assertions.assertEquals(genesisSlot.plus(61), states.get(1).getLatestSlotState().getSlot());
  }
}
