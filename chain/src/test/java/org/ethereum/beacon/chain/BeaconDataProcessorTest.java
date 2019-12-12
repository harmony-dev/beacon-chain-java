package org.ethereum.beacon.chain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import org.ethereum.beacon.chain.eventbus.EventBus;
import org.ethereum.beacon.chain.eventbus.events.AttesterStateUpdated;
import org.ethereum.beacon.chain.eventbus.events.ChainStarted;
import org.ethereum.beacon.chain.eventbus.events.ProposerStateUpdated;
import org.ethereum.beacon.chain.eventbus.events.TimeTick;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.store.TransactionalStore;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconDataProcessorTest {

  @Test
  public void tickOnNewSlot() {
    BeaconChainSpec spec =
        new BeaconChainSpec.Builder()
            .withConstants(BeaconChainSpec.DEFAULT_CONSTANTS)
            .withDefaultHashFunction()
            .withDefaultHasher(BeaconChainSpec.DEFAULT_CONSTANTS)
            .withComputableGenesisTime(false)
            .build();

    Time genesisTime = Time.of(1);
    TransactionalStore store = TransactionalStore.inMemoryStore();

    ControlledSchedulers schedulers = Schedulers.createControlled();
    schedulers.setCurrentTime(0);
    EventBus eventBus = EventBus.create(schedulers);
    BeaconDataProcessor processor = new BeaconDataProcessorImpl(spec, store, eventBus);

    ObservableStateHolder recentState = new ObservableStateHolder();
    eventBus.subscribe(AttesterStateUpdated.class, recentState::set);
    eventBus.subscribe(ProposerStateUpdated.class, recentState::set);

    eventBus.publish(
        ChainStarted.wrap(
            new ChainStart(
                genesisTime,
                new Eth1Data(Hash32.ZERO, UInt64.ZERO, Hash32.ZERO),
                Collections.emptyList())));
    schedulers.addTime(1);

    assertNotNull(recentState.state);
    assertEquals(
        SlotNumber.of(0).plus(spec.getConstants().getGenesisSlot()),
        recentState.state.getLatestSlotState().getSlot());

    eventBus.publish(TimeTick.wrap(genesisTime.plus(spec.getConstants().getSecondsPerSlot())));
    schedulers.addTime(1);

    assertNotNull(recentState.state);
    assertEquals(
        SlotNumber.of(1).plus(spec.getConstants().getGenesisSlot()),
        recentState.state.getLatestSlotState().getSlot());
  }

  private static class ObservableStateHolder {
    private ObservableBeaconState state;

    private void set(ObservableBeaconState state) {
      this.state = state;
    }
  }
}
