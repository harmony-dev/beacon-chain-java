package org.ethereum.beacon.chain;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.store.TransactionalStore;
import org.ethereum.beacon.chain.store.TransactionalStore.StoreTx;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperationsState;
import org.ethereum.beacon.chain.processor.AttestationPool;
import org.ethereum.beacon.chain.processor.AttestationPoolImpl;
import org.ethereum.beacon.chain.processor.DelayedAttestationQueue;
import org.ethereum.beacon.chain.processor.DelayedAttestationQueueImpl;
import org.ethereum.beacon.chain.processor.DelayedBlockQueue;
import org.ethereum.beacon.chain.processor.DelayedBlockQueueImpl;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.consensus.spec.SpecCommons.BlockIsInTheFutureException;
import org.ethereum.beacon.consensus.spec.SpecCommons.EarlyForkChoiceConsiderationException;
import org.ethereum.beacon.consensus.spec.SpecCommons.NoParentBlockException;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconDataProcessorImpl implements BeaconDataProcessor {

  private static final Logger logger = LogManager.getLogger("chain");

  private final BeaconChainSpec spec;
  private final BeaconChainSpec helperFunctions;
  private final BeaconChainSpec forkChoice;
  private final BeaconChainSpec stateTransition;
  private final TransactionalStore store;
  private final AttestationPool attestationPool;
  private final DelayedBlockQueue delayedBlockQueue;
  private final DelayedAttestationQueue delayedAttestationQueue;

  private Consumer<ObservableBeaconState> stateSubscriber;
  private Consumer<BeaconBlock> blockSubscriber;

  private SlotNumber currentSlot;

  public BeaconDataProcessorImpl(BeaconChainSpec spec, TransactionalStore store) {
    this.spec = spec;
    this.helperFunctions = spec;
    this.forkChoice = spec;
    this.stateTransition = spec;
    this.store = store;
    this.attestationPool =
        new AttestationPoolImpl(spec, spec.compute_epoch_at_slot(spec.get_current_slot(store)));

    this.delayedBlockQueue = new DelayedBlockQueueImpl();
    this.delayedAttestationQueue = new DelayedAttestationQueueImpl();

    delayedBlockQueue.subscribe(this::onBlock);
    delayedAttestationQueue.subscribe(this::onAttestation);

    this.currentSlot = forkChoice.get_current_slot(store);
  }

  @Override
  public void onTick(Time time) {
    logger.trace("On before tick: " + time);

    SlotNumber previousSlot = forkChoice.get_current_slot(store);

    StoreTx storeTx = store.newTx();
    forkChoice.on_tick(storeTx, time);

    storeTx.commit();

    SlotNumber currentSlot = forkChoice.get_current_slot(store);
    // tick on new slot
    if (currentSlot.greater(previousSlot)) {
      onTick(currentSlot);
    }

    logger.trace("On after tick: " + time);
  }

  void onTick(SlotNumber slot) {
    this.currentSlot = slot;

    yieldObservableState(slot, TransitionType.SLOT);

    delayedAttestationQueue.onTick(slot);
    attestationPool.onTick(slot);
    delayedBlockQueue.onTick(slot);
  }

  private void yieldObservableState(SlotNumber slot, TransitionType transitionType) {
    if (stateSubscriber != null) {
      Hash32 root = forkChoice.get_head(store);
      Optional<BeaconBlock> block = store.getBlock(root);
      Optional<BeaconState> state = store.getState(root);

      assert block.isPresent() && state.isPresent();

      MutableBeaconState mutableState = state.get().createMutableCopy();
      stateTransition.process_slots(mutableState, slot);
      BeaconStateEx finalState =
          new BeaconStateExImpl(mutableState.createImmutable(), transitionType);
      List<Attestation> attestations = attestationPool.getOffChainAttestations(finalState);

      ObservableBeaconState observableState =
          new ObservableBeaconState(
              block.get(), finalState, new PendingOperationsState(attestations));

      stateSubscriber.accept(observableState);

      logger.trace(
          "Observable state: "
              + observableState
              .getLatestSlotState()
              .toString(spec.getConstants(), spec::signing_root));
    }
  }

  @Override
  public boolean onBlock(BeaconBlock block) {
    logger.trace(
        "On before block: " + block.toString(spec.getConstants(), null, spec::signing_root));

    Optional<BeaconBlock> beforeImport = store.getBlock(helperFunctions.signing_root(block));

    StoreTx storeTx = store.newTx();
    // state transition is a part of on_block
    try {
      forkChoice.on_block(storeTx, block);
      storeTx.commit();
    } catch (BlockIsInTheFutureException e) {
      // queue future blocks
      delayedBlockQueue.onBlock(block);
      logger.debug(
          "Delay block: " + block.toString(spec.getConstants(), null, spec::signing_root));
    } catch (NoParentBlockException e) {
      // handle no parent
      logger.info(
          "No parent: " + block.toString(spec.getConstants(), null, spec::signing_root));
    }

    Optional<BeaconBlock> afterImport = store.getBlock(helperFunctions.signing_root(block));
    boolean newlyImported = !beforeImport.isPresent() && afterImport.isPresent();
    if (newlyImported) {
      logger.info(
          "Imported block: " + block.toString(spec.getConstants(), null, spec::signing_root));
      blockSubscriber.accept(block);

      yieldObservableState(currentSlot, TransitionType.BLOCK);
    }

    logger.trace("On after block: " + block.toString(spec.getConstants(), null, spec::signing_root));

    return newlyImported;
  }

  @Override
  public void onAttestation(Attestation attestation) {
    StoreTx storeTx = store.newTx();

    try {
      forkChoice.on_attestation(storeTx, attestation);
      storeTx.commit();

      // FIXME suboptimal, no need in yielding it each time
      yieldObservableState(currentSlot, TransitionType.UNKNOWN);

      logger.info("Processed attestation: " + attestation);
    } catch (EarlyForkChoiceConsiderationException e) {
      // delay attestation consideration
      delayedAttestationQueue.onAttestation(attestation);
      logger.debug("Delay attestation: " + attestation);
    }

    attestationPool.onAttestation(attestation);
  }

  @Override
  public void subscribeToStates(Consumer<ObservableBeaconState> subscriber) {
    this.stateSubscriber = subscriber;
  }

  @Override
  public void subscribeToBlocks(Consumer<BeaconBlock> subscriber) {
    this.blockSubscriber = subscriber;
  }
}
