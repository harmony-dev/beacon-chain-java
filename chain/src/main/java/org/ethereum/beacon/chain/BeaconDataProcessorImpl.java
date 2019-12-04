package org.ethereum.beacon.chain;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import org.ethereum.beacon.chain.TransactionalStore.StoreTx;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperationsState;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconDataProcessorImpl implements BeaconDataProcessor {

  private final BeaconChainSpec forkChoice;
  private final BeaconChainSpec stateTransition;
  private final TransactionalStore store;

  private Consumer<ObservableBeaconState> stateSubscriber;

  public BeaconDataProcessorImpl(BeaconChainSpec spec, TransactionalStore store) {
    this.forkChoice = spec;
    this.stateTransition = spec;
    this.store = store;
  }

  @Override
  public void onTick(Time time) {
    SlotNumber previousSlot = forkChoice.get_current_slot(store);

    StoreTx storeTx = store.newTx();
    forkChoice.on_tick(storeTx, time);

    storeTx.commit();

    SlotNumber currentSlot = forkChoice.get_current_slot(store);
    // tick on new slot
    if (currentSlot.greater(previousSlot)) {
      onTick(currentSlot);
    }
  }

  void onTick(SlotNumber slot) {
    if (stateSubscriber != null) {
      Hash32 root = forkChoice.get_head(store);
      Optional<BeaconBlock> block = store.getBlock(root);
      Optional<BeaconState> state = store.getState(root);

      assert block.isPresent() && state.isPresent();

      MutableBeaconState mutableState = state.get().createMutableCopy();
      stateTransition.process_slots(mutableState, slot);
      BeaconStateEx finalState = new BeaconStateExImpl(mutableState.createImmutable());

      ObservableBeaconState observableState =
          new ObservableBeaconState(
              block.get(), finalState, new PendingOperationsState(Collections.emptyList()));

      stateSubscriber.accept(observableState);
    }
  }

  @Override
  public void onBlock(BeaconBlock block) {
    StoreTx storeTx = store.newTx();
    // state transition is a part of on_block
    forkChoice.on_block(storeTx, block);

    storeTx.commit();
  }

  @Override
  public void onAttestation(Attestation attestation) {
    StoreTx storeTx = store.newTx();
    forkChoice.on_attestation(storeTx, attestation);

    storeTx.commit();
  }

  @Override
  public void subscribe(Consumer<ObservableBeaconState> subscriber) {
    this.stateSubscriber = subscriber;
  }
}
