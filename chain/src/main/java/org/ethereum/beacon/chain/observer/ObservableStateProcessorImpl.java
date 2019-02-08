package org.ethereum.beacon.chain.observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.LMDGhostHeadFunction;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.HeadFunction;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.transition.BeaconStateEx;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

public class ObservableStateProcessorImpl implements ObservableStateProcessor {
  private final BeaconTupleStorage tupleStorage;
  private ObservableBeaconState observableState;
  private BeaconChainHead head;
  private final HeadFunction headFunction;

  private BeaconState latestState;
  private final SpecHelpers specHelpers;
  private final ChainSpec chainSpec;
  private final EmptySlotTransition emptySlotTransition;

  private final Publisher<SlotNumber> slotTicker;
  private final Publisher<Attestation> attestationPublisher;
  private final Publisher<BeaconTuple> beaconPublisher;

  private static final int UPDATE_MILLIS = 500;
  private ScheduledExecutorService regularJobExecutor;
  private ScheduledExecutorService continuousJobExecutor;
  private final BlockingQueue<Attestation> attestationBuffer = new LinkedBlockingDeque<>();
  private final Map<BLSPubkey, Attestation> attestationCache = new HashMap<>();
  private final Map<UInt64, Set<BLSPubkey>> validatorSlotCache = new HashMap<>();

  private final ReplayProcessor<BeaconChainHead> headSink = ReplayProcessor.cacheLast();
  private final Publisher<BeaconChainHead> headStream =
      Flux.from(headSink)
          .publishOn(Schedulers.single())
          .onBackpressureError()
          .name("ObservableStateProcessor.head");

  private final ReplayProcessor<ObservableBeaconState> observableStateSink =
      ReplayProcessor.cacheLast();
  private final Publisher<ObservableBeaconState> observableStateStream =
      Flux.from(observableStateSink)
          .publishOn(Schedulers.single())
          .onBackpressureError()
          .name("ObservableStateProcessor.observableState");

  private final ReplayProcessor<PendingOperations> pendingOperationsSink =
      ReplayProcessor.cacheLast();
  private final Publisher<PendingOperations> pendingOperationsStream =
      Flux.from(pendingOperationsSink)
          .publishOn(Schedulers.single())
          .onBackpressureError()
          .name("PendingOperationsProcessor.pendingOperations");

  public ObservableStateProcessorImpl(
      BeaconChainStorage chainStorage,
      Publisher<SlotNumber> slotTicker,
      Publisher<Attestation> attestationPublisher,
      Publisher<BeaconTuple> beaconPublisher,
      SpecHelpers specHelpers,
      EmptySlotTransition emptySlotTransition) {
    this.tupleStorage = chainStorage.getBeaconTupleStorage();
    this.specHelpers = specHelpers;
    this.chainSpec = specHelpers.getChainSpec();
    this.emptySlotTransition = emptySlotTransition;
    this.headFunction = new LMDGhostHeadFunction(chainStorage, specHelpers);
    this.slotTicker = slotTicker;
    this.attestationPublisher = attestationPublisher;
    this.beaconPublisher = beaconPublisher;
  }

  @Override
  public void start() {
    Flux.from(slotTicker).doOnNext(this::onNewSlot).subscribe();
    Flux.from(attestationPublisher).doOnNext(this::onNewAttestation).subscribe();
    Flux.from(beaconPublisher).doOnNext(this::onNewBlockTuple).subscribe();
    this.regularJobExecutor =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread t = new Thread(runnable, "observable-state-processor-regular");
              t.setDaemon(true);
              return t;
            });
    regularJobExecutor.scheduleAtFixedRate(
        this::doHardWork, 0, UPDATE_MILLIS, TimeUnit.MILLISECONDS);
    this.continuousJobExecutor =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread t = new Thread(runnable, "observable-state-processor-continuous");
              t.setDaemon(true);
              return t;
            });
  }

  private void runTaskInSeparateThread(Runnable task) {
    continuousJobExecutor.execute(task);
  }

  private void onNewSlot(SlotNumber newSlot) {
    // From spec: Verify that attestation.data.slot <= state.slot - MIN_ATTESTATION_INCLUSION_DELAY
    // < attestation.data.slot + EPOCH_LENGTH
    // state.slot - MIN_ATTESTATION_INCLUSION_DELAY < attestation.data.slot + EPOCH_LENGTH
    // state.slot - MIN_ATTESTATION_INCLUSION_DELAY - EPOCH_LENGTH < attestation.data.slot
    SlotNumber slotMinimum =
        newSlot
            .minus(chainSpec.getEpochLength())
            .minus(chainSpec.getMinAttestationInclusionDelay());
    runTaskInSeparateThread(
        () -> {
          purgeAttestations(slotMinimum);
          updateCurrentObservableState(newSlot);
        });
  }

  private void doHardWork() {
    List<Attestation> attestations = new ArrayList<>();
    attestationBuffer.drainTo(attestations);
    for (Attestation attestation : attestations) {

      List<ValidatorIndex> participants =
          specHelpers.get_attestation_participants(
              latestState, attestation.getData(), attestation.getParticipationBitfield());

      List<BLSPubkey> pubKeys = specHelpers.mapIndicesToPubKeys(latestState, participants);

      for (BLSPubkey pubKey : pubKeys) {
        addValidatorAttestation(pubKey, attestation);
      }
    }
  }

  private synchronized void addValidatorAttestation(BLSPubkey pubKey, Attestation attestation) {
    if (attestationCache.containsKey(pubKey)) {
      Attestation oldAttestation = attestationCache.get(pubKey);
      if (attestation.getData().getSlot().compareTo(oldAttestation.getData().getSlot()) > 0) {
        attestationCache.put(pubKey, attestation);
        validatorSlotCache.get(oldAttestation.getData().getSlot()).remove(pubKey);
        addToSlotCache(attestation.getData().getSlot(), pubKey);
      } else {
        // XXX: If several such attestations exist, use the one the validator v observed first
        // so no need to swap it
      }
    } else {
      attestationCache.put(pubKey, attestation);
      addToSlotCache(attestation.getData().getSlot(), pubKey);
    }
  }

  private void addToSlotCache(UInt64 slot, BLSPubkey pubKey) {
    if (validatorSlotCache.containsKey(slot)) {
      validatorSlotCache.get(slot).add(pubKey);
    } else {
      Set<BLSPubkey> pubKeysSet = new HashSet<>();
      pubKeysSet.add(pubKey);
      validatorSlotCache.put(slot, pubKeysSet);
    }
  }

  private void onNewAttestation(Attestation attestation) {
    runTaskInSeparateThread(() -> attestationBuffer.add(attestation));
  }

  private void onNewBlockTuple(BeaconTuple beaconTuple) {
    this.latestState = beaconTuple.getState();
    runTaskInSeparateThread(() -> addAttestationsFromState(beaconTuple.getState()));
  }

  private void addAttestationsFromState(BeaconState beaconState) {
    ReadList<Integer, PendingAttestationRecord> pendingAttestationRecords =
        beaconState.getLatestAttestations();
    for (PendingAttestationRecord pendingAttestationRecord : pendingAttestationRecords) {
      List<ValidatorIndex> participants =
          specHelpers.get_attestation_participants(
              beaconState,
              pendingAttestationRecord.getData(),
              pendingAttestationRecord.getParticipationBitfield());
      List<BLSPubkey> pubKeys = specHelpers.mapIndicesToPubKeys(beaconState, participants);
      SlotNumber slot = pendingAttestationRecord.getData().getSlot();
      pubKeys.forEach(
          pubKey -> {
            removeValidatorAttestation(pubKey, slot);
          });
    }
  }

  private synchronized void removeValidatorAttestation(BLSPubkey pubkey, SlotNumber slot) {
    attestationCache.remove(pubkey);
    if (validatorSlotCache.containsKey(slot)) {
      validatorSlotCache.get(slot).remove(pubkey);
    }
  }

  /** Purges all entries for slot and before */
  private synchronized void purgeAttestations(UInt64 slot) {
    for (Map.Entry<UInt64, Set<BLSPubkey>> entry : validatorSlotCache.entrySet()) {
      if (entry.getKey().compareTo(slot) <= 0) {
        entry.getValue().forEach(attestationCache::remove);
        validatorSlotCache.remove(entry.getKey());
      }
    }
  }

  private synchronized Map<BLSPubkey, Attestation> drainAttestationCache() {
    return new HashMap<>(attestationCache);
  }

  private void updateCurrentObservableState(SlotNumber newSlot) {
    PendingOperations pendingOperations = new PendingOperationsState(drainAttestationCache());
    pendingOperationsSink.onNext(pendingOperations);
    updateHead(pendingOperations);
    this.latestState =
        applySlotTransitions(latestState, newSlot, specHelpers.hash_tree_root(head.getBlock()));
    ObservableBeaconState newObservableState =
        new ObservableBeaconState(head.getBlock(), latestState, pendingOperations);
    if (!newObservableState.equals(observableState)) {
      this.observableState = newObservableState;
      observableStateSink.onNext(newObservableState);
    }
  }

  /**
   * Applies next slot transition and if it's required - epoch transition
   *
   * @param canonicalState Source state
   * @param newSlot Slot number of the new slot
   * @param latestChainBlock Latest chain block
   * @return new state, result of applied transition to the latest input state
   */
  private BeaconState applySlotTransitions(
      BeaconState canonicalState, SlotNumber newSlot, Hash32 latestChainBlock) {
    BeaconStateEx result =
        emptySlotTransition.apply(new BeaconStateEx(canonicalState, latestChainBlock), newSlot);
    return result.getCanonicalState();
  }

  private void updateHead(PendingOperations pendingOperations) {
    BeaconBlock newHead =
        headFunction.getHead(
            validatorRecord -> pendingOperations.findAttestation(validatorRecord.getPubKey()));
    if (this.head != null && this.head.getBlock().equals(newHead)) {
      return; // == old
    }
    BeaconTuple newHeadTuple =
        tupleStorage
            .get(specHelpers.hash_tree_root(newHead))
            .orElseThrow(() -> new IllegalStateException("Beacon tuple not found for new head "));
    this.head = BeaconChainHead.of(newHeadTuple);

    headSink.onNext(this.head);
  }

  @Override
  public Publisher<BeaconChainHead> getHeadStream() {
    return headStream;
  }

  @Override
  public Publisher<ObservableBeaconState> getObservableStateStream() {
    return observableStateStream;
  }

  @Override
  public Publisher<PendingOperations> getPendingOperationsStream() {
    return pendingOperationsStream;
  }
}
