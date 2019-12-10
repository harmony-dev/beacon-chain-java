package org.ethereum.beacon.chain;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.eventbus.EventBus;
import org.ethereum.beacon.chain.eventbus.events.AttestationConsiderationDelayed;
import org.ethereum.beacon.chain.eventbus.events.AttestationProduced;
import org.ethereum.beacon.chain.eventbus.events.AttestationReceived;
import org.ethereum.beacon.chain.eventbus.events.AttestationUnparked;
import org.ethereum.beacon.chain.eventbus.events.BlockConsiderationDelayed;
import org.ethereum.beacon.chain.eventbus.events.BlockImported;
import org.ethereum.beacon.chain.eventbus.events.BlockProposed;
import org.ethereum.beacon.chain.eventbus.events.BlockReceived;
import org.ethereum.beacon.chain.eventbus.events.BlockUnparked;
import org.ethereum.beacon.chain.eventbus.events.ObservableStateUpdated;
import org.ethereum.beacon.chain.eventbus.events.ProposedBlockImported;
import org.ethereum.beacon.chain.eventbus.events.SlotTick;
import org.ethereum.beacon.chain.eventbus.events.TimeTick;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperationsState;
import org.ethereum.beacon.chain.processor.AttestationPool;
import org.ethereum.beacon.chain.processor.AttestationPoolImpl;
import org.ethereum.beacon.chain.processor.DelayedAttestationQueue;
import org.ethereum.beacon.chain.processor.DelayedAttestationQueueImpl;
import org.ethereum.beacon.chain.processor.DelayedBlockQueue;
import org.ethereum.beacon.chain.processor.DelayedBlockQueueImpl;
import org.ethereum.beacon.chain.store.TransactionalStore;
import org.ethereum.beacon.chain.store.TransactionalStore.StoreTx;
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

  private SlotNumber currentSlot;

  private EventBus eventBus;

  public BeaconDataProcessorImpl(
      BeaconChainSpec spec, TransactionalStore store, EventBus eventBus) {
    this.spec = spec;
    this.helperFunctions = spec;
    this.forkChoice = spec;
    this.stateTransition = spec;
    this.store = store;
    this.eventBus = eventBus;
    this.attestationPool =
        new AttestationPoolImpl(spec, spec.compute_epoch_at_slot(spec.get_current_slot(store)));

    this.delayedBlockQueue = new DelayedBlockQueueImpl(this.eventBus);
    this.delayedAttestationQueue = new DelayedAttestationQueueImpl(this.eventBus);

    this.currentSlot = forkChoice.get_current_slot(store);

    this.eventBus.subscribe(TimeTick.class, this::onTick);
    this.eventBus.subscribe(BlockReceived.class, this::onBlock);
    this.eventBus.subscribe(BlockUnparked.class, this::onBlock);
    this.eventBus.subscribe(BlockProposed.class, this::onBlockProposed);
    this.eventBus.subscribe(AttestationReceived.class, this::onAttestation);
    this.eventBus.subscribe(AttestationUnparked.class, this::onAttestation);
    this.eventBus.subscribe(AttestationProduced.class, this::onAttestation);

    this.eventBus.subscribe(SlotTick.class, attestationPool::onTick);
    this.eventBus.subscribe(AttestationReceived.class, attestationPool::onAttestation);
    this.eventBus.subscribe(AttestationProduced.class, attestationPool::onAttestation);

    this.eventBus.subscribe(SlotTick.class, delayedAttestationQueue::onTick);
    this.eventBus.subscribe(
        AttestationConsiderationDelayed.class, delayedAttestationQueue::onAttestation);

    this.eventBus.subscribe(SlotTick.class, delayedBlockQueue::onTick);
    this.eventBus.subscribe(BlockConsiderationDelayed.class, delayedBlockQueue::onBlock);
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
    eventBus.publish(SlotTick.wrap(slot));
    yieldObservableState(slot, TransitionType.SLOT);
  }

  private void yieldObservableState(SlotNumber slot, TransitionType transitionType) {
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

    eventBus.publish(ObservableStateUpdated.wrap(observableState));

    logger.trace(
        "Observable state: "
            + observableState
                .getLatestSlotState()
                .toString(spec.getConstants(), spec::signing_root));
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
      eventBus.publish(BlockConsiderationDelayed.wrap(block));
      logger.debug("Delay block: " + block.toString(spec.getConstants(), null, spec::signing_root));
    } catch (NoParentBlockException e) {
      // handle no parent
      logger.info("No parent: " + block.toString(spec.getConstants(), null, spec::signing_root));
    }

    Optional<BeaconBlock> afterImport = store.getBlock(helperFunctions.signing_root(block));
    boolean newlyImported = !beforeImport.isPresent() && afterImport.isPresent();
    if (newlyImported) {
      eventBus.publish(BlockImported.wrap(block));
      yieldObservableState(currentSlot, TransitionType.BLOCK);

      logger.info(
          "Imported block: " + block.toString(spec.getConstants(), null, spec::signing_root));
    }

    logger.trace(
        "On after block: " + block.toString(spec.getConstants(), null, spec::signing_root));

    return newlyImported;
  }

  private void onBlockProposed(BeaconBlock block) {
    if (onBlock(block)) {
      eventBus.publish(ProposedBlockImported.wrap(block));
    }
  }

  @Override
  public void onAttestation(Attestation attestation) {
    StoreTx storeTx = store.newTx();

    try {
      forkChoice.on_attestation(storeTx, attestation);
      storeTx.commit();

      // FIXME suboptimal, no need to compute observable state on each attestation
      yieldObservableState(currentSlot, TransitionType.UNKNOWN);

      logger.info("Processed attestation: " + attestation);
    } catch (EarlyForkChoiceConsiderationException e) {
      // delay attestation consideration
      eventBus.publish(AttestationConsiderationDelayed.wrap(attestation));
      logger.debug("Delay attestation: " + attestation);
    }
  }
}
