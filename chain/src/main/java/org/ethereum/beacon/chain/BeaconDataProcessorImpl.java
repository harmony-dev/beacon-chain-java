package org.ethereum.beacon.chain;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.eventbus.EventBus;
import org.ethereum.beacon.chain.eventbus.events.AttestationBatchDequeued;
import org.ethereum.beacon.chain.eventbus.events.AttestationBatchReceived;
import org.ethereum.beacon.chain.eventbus.events.AttestationConsiderationDelayed;
import org.ethereum.beacon.chain.eventbus.events.AttestationMetNoBlockRoot;
import org.ethereum.beacon.chain.eventbus.events.AttestationMetNoTargetRoot;
import org.ethereum.beacon.chain.eventbus.events.AttestationProduced;
import org.ethereum.beacon.chain.eventbus.events.AttestationReceived;
import org.ethereum.beacon.chain.eventbus.events.AttestationTargetEpochHasNotCome;
import org.ethereum.beacon.chain.eventbus.events.BlockBatchDequeued;
import org.ethereum.beacon.chain.eventbus.events.BlockConsiderationDelayed;
import org.ethereum.beacon.chain.eventbus.events.BlockImported;
import org.ethereum.beacon.chain.eventbus.events.BlockMetNoParent;
import org.ethereum.beacon.chain.eventbus.events.BlockProposed;
import org.ethereum.beacon.chain.eventbus.events.BlockReceived;
import org.ethereum.beacon.chain.eventbus.events.ChainStarted;
import org.ethereum.beacon.chain.eventbus.events.ProposedBlockImported;
import org.ethereum.beacon.chain.eventbus.events.SlotTick;
import org.ethereum.beacon.chain.eventbus.events.StateAtTheBeginningOfSlotYielded;
import org.ethereum.beacon.chain.eventbus.events.StateThroughoutSlotYielded;
import org.ethereum.beacon.chain.eventbus.events.TimeTick;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperationsState;
import org.ethereum.beacon.chain.processor.AttestationPool;
import org.ethereum.beacon.chain.processor.AttestationPoolImpl;
import org.ethereum.beacon.chain.processor.DelayedAttestationQueue;
import org.ethereum.beacon.chain.processor.DelayedAttestationQueueImpl;
import org.ethereum.beacon.chain.processor.DelayedBlockQueue;
import org.ethereum.beacon.chain.processor.DelayedBlockQueueImpl;
import org.ethereum.beacon.chain.processor.DelayedUntilTargetEpochQueue;
import org.ethereum.beacon.chain.processor.DelayedUntilTargetEpochQueueImpl;
import org.ethereum.beacon.chain.processor.NoBlockRootAttestationQueue;
import org.ethereum.beacon.chain.processor.NoBlockRootAttestationQueueImpl;
import org.ethereum.beacon.chain.processor.NoParentBlockQueue;
import org.ethereum.beacon.chain.processor.NoParentBlockQueueImpl;
import org.ethereum.beacon.chain.processor.NoTargetRootAttestationQueue;
import org.ethereum.beacon.chain.processor.NoTargetRootAttestationQueueImpl;
import org.ethereum.beacon.chain.store.TransactionalStore;
import org.ethereum.beacon.chain.store.TransactionalStore.StoreTx;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.consensus.spec.ForkChoice.BlockIsInTheFutureException;
import org.ethereum.beacon.consensus.spec.ForkChoice.EarlyForkChoiceConsiderationException;
import org.ethereum.beacon.consensus.spec.ForkChoice.NoBlockRootException;
import org.ethereum.beacon.consensus.spec.ForkChoice.NoParentBlockException;
import org.ethereum.beacon.consensus.spec.ForkChoice.NoTargetRootException;
import org.ethereum.beacon.consensus.spec.ForkChoice.TargetEpochIsInTheFutureException;
import org.ethereum.beacon.consensus.spec.SpecCommons.SpecAssertionFailed;
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
  private TransactionalStore store;
  private final EventBus eventBus;

  private final AttestationPool attestationPool;
  private final DelayedBlockQueue delayedBlockQueue;
  private final DelayedAttestationQueue delayedAttestationQueue;
  private final NoParentBlockQueue noParentBlockQueue;
  private final NoTargetRootAttestationQueue noTargetRootAttestationQueue;
  private final NoBlockRootAttestationQueue noBlockRootAttestationQueue;
  private final DelayedUntilTargetEpochQueue delayedUntilTargetEpochQueue;

  public BeaconDataProcessorImpl(
      BeaconChainSpec spec, TransactionalStore store, EventBus eventBus) {
    this.spec = spec;
    this.helperFunctions = spec;
    this.forkChoice = spec;
    this.stateTransition = spec;
    this.store = store;
    this.eventBus = eventBus;

    this.attestationPool = new AttestationPoolImpl(this.eventBus, this.spec);
    this.delayedBlockQueue = new DelayedBlockQueueImpl(this.eventBus);
    this.delayedAttestationQueue = new DelayedAttestationQueueImpl(this.eventBus);
    this.noParentBlockQueue = new NoParentBlockQueueImpl(this.eventBus, this.spec);
    this.noTargetRootAttestationQueue =
        new NoTargetRootAttestationQueueImpl(this.eventBus, this.spec);
    this.noBlockRootAttestationQueue =
        new NoBlockRootAttestationQueueImpl(this.eventBus, this.spec);
    this.delayedUntilTargetEpochQueue =
        new DelayedUntilTargetEpochQueueImpl(this.eventBus, this.spec);

    this.eventBus.subscribe(ChainStarted.class, this::onChainStart);
    this.eventBus.subscribe(TimeTick.class, this::onTick);
    this.eventBus.subscribe(SlotTick.class, this::onTick);
    this.eventBus.subscribe(BlockReceived.class, this::onBlock);
    this.eventBus.subscribe(BlockProposed.class, this::onBlockProposed);
    this.eventBus.subscribe(BlockBatchDequeued.class, this::onBlocksDequeued);
    this.eventBus.subscribe(AttestationReceived.class, this::onAttestation);
    this.eventBus.subscribe(AttestationProduced.class, this::onAttestation);
    this.eventBus.subscribe(AttestationBatchReceived.class, this::onAttestations);
    this.eventBus.subscribe(AttestationBatchDequeued.class, this::onAttestations);

    this.eventBus.subscribe(SlotTick.class, attestationPool::onTick);
    this.eventBus.subscribe(AttestationReceived.class, attestationPool::onAttestation);
    this.eventBus.subscribe(AttestationProduced.class, attestationPool::onAttestation);
    this.eventBus.subscribe(
        StateAtTheBeginningOfSlotYielded.class, attestationPool::onStateAtTheBeginningOfSlot);

    this.eventBus.subscribe(SlotTick.class, delayedAttestationQueue::onTick);
    this.eventBus.subscribe(
        AttestationConsiderationDelayed.class, delayedAttestationQueue::onAttestation);

    this.eventBus.subscribe(SlotTick.class, delayedBlockQueue::onTick);
    this.eventBus.subscribe(BlockConsiderationDelayed.class, delayedBlockQueue::onBlock);

    this.eventBus.subscribe(BlockMetNoParent.class, noParentBlockQueue::onBlockWithNoParent);
    this.eventBus.subscribe(BlockImported.class, noParentBlockQueue::onImportedBlock);

    this.eventBus.subscribe(
        AttestationMetNoTargetRoot.class, noTargetRootAttestationQueue::onAttestation);
    this.eventBus.subscribe(BlockImported.class, noTargetRootAttestationQueue::onBlock);

    this.eventBus.subscribe(
        AttestationMetNoBlockRoot.class, noBlockRootAttestationQueue::onAttestation);
    this.eventBus.subscribe(BlockImported.class, noBlockRootAttestationQueue::onBlock);

    this.eventBus.subscribe(
        AttestationTargetEpochHasNotCome.class, delayedUntilTargetEpochQueue::onAttestation);
    this.eventBus.subscribe(SlotTick.class, delayedUntilTargetEpochQueue::onTick);
  }

  @Override
  public void onTick(Time time) {
    logger.trace("On before tick: " + time);

    // do not process tick if store is not yet initialized
    if (!store.isInitialized()) {
      return;
    }

    // do not process tick at genesis time
    if (time.equals(store.getGenesisTime())) {
      return;
    }

    SlotNumber previousSlot = forkChoice.get_current_slot(store);

    StoreTx storeTx = store.newTx();
    forkChoice.on_tick(storeTx, time);

    storeTx.commit();

    SlotNumber currentSlot = forkChoice.get_current_slot(store);
    if (currentSlot.greater(previousSlot)) {
      eventBus.publish(SlotTick.wrap(currentSlot));
    }

    logger.trace("On after tick: " + time);
  }

  void onChainStart(ChainStart chainStart) {
    assert !store.isInitialized();

    BeaconState genesisState =
        spec.initialize_beacon_state_from_eth1(
            chainStart.getEth1Data().getBlockHash(),
            chainStart.getTime(),
            chainStart.getInitialDeposits());
    store = spec.get_genesis_store(genesisState, store);

    // tick on genesis slot
    eventBus.publish(SlotTick.wrap(genesisState.getSlot()));
  }

  void onTick(SlotNumber slot) {
    yieldStateAtTheBeginningOfSlot();
  }

  void yieldStateAtTheBeginningOfSlot() {
    ObservableBeaconState stateAtTheTip = computeStateAtTheTip(TransitionType.SLOT);
    eventBus.publish(StateAtTheBeginningOfSlotYielded.wrap(stateAtTheTip));

    logger.trace(
        "Yield state at the beginning of slot: "
            + stateAtTheTip.getLatestSlotState().toString(spec.getConstants(), spec::signing_root));
  }

  void yieldStateThroughoutSlot() {
    ObservableBeaconState stateAtTheTip = computeStateAtTheTip(TransitionType.UNKNOWN);
    eventBus.publish(StateThroughoutSlotYielded.wrap(stateAtTheTip));

    logger.trace(
        "Yield state throughout a slot: "
            + stateAtTheTip.getLatestSlotState().toString(spec.getConstants(), spec::signing_root));
  }

  ObservableBeaconState computeStateAtTheTip(TransitionType transitionType) {
    Hash32 root = forkChoice.get_head(store);
    Optional<BeaconBlock> block = store.getBlock(root);
    Optional<BeaconState> state = store.getState(root);

    assert block.isPresent() && state.isPresent();

    MutableBeaconState mutableState = state.get().createMutableCopy();
    stateTransition.process_slots(mutableState, forkChoice.get_current_slot(store));
    BeaconStateEx finalState =
        new BeaconStateExImpl(mutableState.createImmutable(), transitionType);

    return new ObservableBeaconState(
        block.get(), finalState, new PendingOperationsState(Collections.emptyList()));
  }

  @Override
  public void onBlock(BeaconBlock block) {
    // first of all, put all attestation to event bus in order to get them processed later
    eventBus.publish(AttestationBatchReceived.wrap(block.getBody().getAttestations().listCopy()));
    onBlockImpl(block);
  }

  boolean onBlockImpl(BeaconBlock block) {
    Optional<BeaconBlock> beforeImport = store.getBlock(helperFunctions.signing_root(block));

    StoreTx storeTx = store.newTx();
    // state transition is a part of on_block
    try {
      forkChoice.on_block(storeTx, block);
      storeTx.commit();

      Optional<BeaconBlock> afterImport = store.getBlock(helperFunctions.signing_root(block));
      boolean newlyImported = !beforeImport.isPresent() && afterImport.isPresent();
      if (newlyImported) {
        eventBus.publish(BlockImported.wrap(block));
        yieldStateThroughoutSlot();

        logger.info(
            "Imported block: " + block.toString(spec.getConstants(), null, spec::signing_root));
      }

      return newlyImported;
    } catch (BlockIsInTheFutureException e) {
      // queue future blocks
      eventBus.publish(BlockConsiderationDelayed.wrap(block));
      logger.debug("Delay block: " + block.toString(spec.getConstants(), null, spec::signing_root));
      return false;
    } catch (NoParentBlockException e) {
      // handle no parent
      eventBus.publish(BlockMetNoParent.wrap(block));
      logger.info("No parent: " + block.toString(spec.getConstants(), null, spec::signing_root));
      return false;
    } catch (SpecAssertionFailed e) {
      logger.error(
          "Failed to import a block: "
              + block.toStringFull(spec.getConstants(), Time.ZERO, spec::signing_root),
          e);
      return false;
    }
  }

  void onBlockProposed(BeaconBlock block) {
    if (onBlockImpl(block)) {
      eventBus.publish(ProposedBlockImported.wrap(block));
    }
  }

  void onBlocksDequeued(Collection<BeaconBlock> blocks) {
    boolean atLeastOneWasImported = blocks.stream().anyMatch(this::onBlockImpl);
    if (atLeastOneWasImported) {
      yieldStateThroughoutSlot();
    }
  }

  @Override
  public void onAttestation(Attestation attestation) {
    if (onAttestationImpl(attestation)) {
      yieldStateThroughoutSlot();
    }
  }

  void onAttestations(Collection<Attestation> attestations) {
    boolean atLeastOneWasApplied = attestations.stream().anyMatch(this::onAttestationImpl);
    if (atLeastOneWasApplied) {
      yieldStateThroughoutSlot();
    }
  }

  boolean onAttestationImpl(Attestation attestation) {
    StoreTx storeTx = store.newTx();

    try {
      forkChoice.on_attestation(storeTx, attestation);
      storeTx.commit();
      logger.info("Processed attestation: " + attestation);
      return true;
    } catch (EarlyForkChoiceConsiderationException e) {
      // delay attestation consideration
      eventBus.publish(AttestationConsiderationDelayed.wrap(attestation));
      logger.debug("Delay attestation: " + attestation);
      return false;
    } catch (NoTargetRootException e) {
      eventBus.publish(AttestationMetNoTargetRoot.wrap(attestation));
      logger.info("No target root found for attestation: " + attestation);
      return false;
    } catch (TargetEpochIsInTheFutureException e) {
      eventBus.publish(AttestationTargetEpochHasNotCome.wrap(attestation));
      logger.info("Target epoch has not yet come: " + attestation);
      return false;
    } catch (NoBlockRootException e) {
      eventBus.publish(AttestationMetNoBlockRoot.wrap(attestation));
      logger.info("No block root found for attestation: " + attestation);
      return false;
    } catch (SpecAssertionFailed e) {
      logger.error("Failed to process an attestation: " + attestation, e);
      return false;
    } catch (RuntimeException e) {
      logger.error("Failed to process an attestation: " + attestation, e);
      return false;
    }
  }
}
