package org.ethereum.beacon.chain.observer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.LMDGhostHeadFunction;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.HeadFunction;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import tech.pegasys.artemis.util.collections.ReadList;

public class ObservableStateProcessorImpl implements ObservableStateProcessor {
  private final BeaconTupleStorage tupleStorage;
  private BeaconChainHead head;
  private final HeadFunction headFunction;

  private BeaconStateEx latestState;
  private SlotNumber latestSlot;

  private final SpecHelpers specHelpers;
  private final ChainSpec chainSpec;
  private final StateTransition<BeaconStateEx> perSlotTransition;
  private final StateTransition<BeaconStateEx> perEpochTransition;

  private final Publisher<SlotNumber> slotTicker;
  private final Publisher<Attestation> attestationPublisher;
  private final Publisher<BeaconTuple> beaconPublisher;

  private static final int UPDATE_MILLIS = 500;
  private Scheduler regularJobExecutor;
  private Scheduler continuousJobExecutor;

  private final List<Attestation> attestationBuffer = new ArrayList<>();
  private final Map<Pair<BLSPubkey, SlotNumber>, Attestation> attestationCache = new HashMap<>();
  private final Schedulers schedulers;

  private final ReplayProcessor<BeaconChainHead> headSink = ReplayProcessor.cacheLast();
  private final Publisher<BeaconChainHead> headStream;

  private final ReplayProcessor<ObservableBeaconState> observableStateSink =
      ReplayProcessor.cacheLast();
  private final Publisher<ObservableBeaconState> observableStateStream;

  private final ReplayProcessor<PendingOperations> pendingOperationsSink =
      ReplayProcessor.cacheLast();
  private final Publisher<PendingOperations> pendingOperationsStream;

  public ObservableStateProcessorImpl(
      BeaconChainStorage chainStorage,
      Publisher<SlotNumber> slotTicker,
      Publisher<Attestation> attestationPublisher,
      Publisher<BeaconTuple> beaconPublisher,
      SpecHelpers specHelpers,
      StateTransition<BeaconStateEx> perSlotTransition,
      StateTransition<BeaconStateEx> perEpochTransition,
      Schedulers schedulers) {
    this.tupleStorage = chainStorage.getTupleStorage();
    this.specHelpers = specHelpers;
    this.chainSpec = specHelpers.getChainSpec();
    this.perSlotTransition = perSlotTransition;
    this.perEpochTransition = perEpochTransition;
    this.headFunction = new LMDGhostHeadFunction(chainStorage, specHelpers);
    this.slotTicker = slotTicker;
    this.attestationPublisher = attestationPublisher;
    this.beaconPublisher = beaconPublisher;
    this.schedulers = schedulers;

    headStream = Flux.from(headSink)
        .publishOn(this.schedulers.reactorEvents())
        .onBackpressureError()
        .name("ObservableStateProcessor.head");
    observableStateStream = Flux.from(observableStateSink)
        .publishOn(this.schedulers.reactorEvents())
        .onBackpressureError()
        .name("ObservableStateProcessor.observableState");
    pendingOperationsStream = Flux.from(pendingOperationsSink)
            .publishOn(this.schedulers.reactorEvents())
            .onBackpressureError()
            .name("PendingOperationsProcessor.pendingOperations");
  }

  @Override
  public void start() {
    regularJobExecutor =
        schedulers.newSingleThreadDaemon("observable-state-processor-regular");
    continuousJobExecutor =
        schedulers.newSingleThreadDaemon("observable-state-processor-continuous");
    Flux.from(slotTicker).subscribe(this::onNewSlot);
    Flux.from(attestationPublisher).subscribe(this::onNewAttestation);
    Flux.from(beaconPublisher).subscribe(this::onNewBlockTuple);
    regularJobExecutor.executeAtFixedRate(
        Duration.ZERO, Duration.ofMillis(UPDATE_MILLIS), this::doHardWork);
  }

  private void runTaskInSeparateThread(Runnable task) {
    continuousJobExecutor.execute(task::run);
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
    List<Attestation> attestations = drainAttestations(latestState.getSlot());
    for (Attestation attestation : attestations) {

      List<ValidatorIndex> participants =
          specHelpers.get_attestation_participants(
              latestState, attestation.getData(), attestation.getAggregationBitfield());

      List<BLSPubkey> pubKeys = specHelpers.mapIndicesToPubKeys(latestState, participants);

      for (BLSPubkey pubKey : pubKeys) {
        addValidatorAttestation(pubKey, attestation);
      }
    }
  }

  private synchronized void addValidatorAttestation(BLSPubkey pubKey, Attestation attestation) {
    attestationCache.put(Pair.with(pubKey, attestation.getData().getSlot()), attestation);
  }

  private synchronized void onNewAttestation(Attestation attestation) {
    attestationBuffer.add(attestation);
  }

  private synchronized List<Attestation> drainAttestations(SlotNumber uptoSlotInclusive) {
    List<Attestation> ret = new ArrayList<>();
    Iterator<Attestation> it = attestationBuffer.iterator();
    while (it.hasNext()) {
      Attestation att = it.next();
      if (att.getData().getSlot().lessEqual(uptoSlotInclusive)) {
        ret.add(att);
        it.remove();
      }
    }
    return ret;
  }


  private void onNewBlockTuple(BeaconTuple beaconTuple) {
    this.latestState = beaconTuple.getState();
    runTaskInSeparateThread(() -> {
      addAttestationsFromState(beaconTuple.getState());
      updateCurrentObservableState(latestSlot);
    });
  }

  private void addAttestationsFromState(BeaconState beaconState) {
    ReadList<Integer, PendingAttestationRecord> pendingAttestationRecords =
        beaconState.getLatestAttestations();
    for (PendingAttestationRecord pendingAttestationRecord : pendingAttestationRecords) {
      List<ValidatorIndex> participants =
          specHelpers.get_attestation_participants(
              beaconState,
              pendingAttestationRecord.getData(),
              pendingAttestationRecord.getAggregationBitfield());
      List<BLSPubkey> pubKeys = specHelpers.mapIndicesToPubKeys(beaconState, participants);
      SlotNumber slot = pendingAttestationRecord.getData().getSlot();
      pubKeys.forEach(
          pubKey -> {
            removeValidatorAttestation(pubKey, slot);
          });
    }
  }

  private synchronized void removeValidatorAttestation(BLSPubkey pubkey, SlotNumber slot) {
    attestationCache.remove(Pair.with(pubkey, slot));
  }

  /** Purges all entries for slot and before */
  private synchronized void purgeAttestations(SlotNumber slot) {
    attestationCache.entrySet()
        .removeIf(entry -> entry.getValue().getData().getSlot().lessEqual(slot));
  }

  private synchronized Map<BLSPubkey, List<Attestation>> copyAttestationCache() {
    return attestationCache.entrySet().stream()
        .collect(
            Collectors.groupingBy(
                e -> e.getKey().getValue0(),
                Collectors.mapping(Entry::getValue, Collectors.toList())));
  }

  private void updateCurrentObservableState(SlotNumber newSlot) {
    if (newSlot == null) {
      return;
    }
    latestSlot = newSlot;
    PendingOperations pendingOperations = new PendingOperationsState(copyAttestationCache());
    pendingOperationsSink.onNext(pendingOperations);
    updateHead(pendingOperations);

    BeaconStateEx originalState = latestState;
    BeaconStateEx stateWithoutEpoch = applySlotTransitionsWithoutEpoch(originalState, newSlot);

    latestState = stateWithoutEpoch;
    observableStateSink.onNext(new ObservableBeaconState(
        head.getBlock(), stateWithoutEpoch, pendingOperations));

    BeaconStateEx epochState = applyEpochTransitionIfNeeded(originalState, stateWithoutEpoch);
    if (epochState != null) {
      latestState = epochState;
      observableStateSink.onNext(new ObservableBeaconState(
          head.getBlock(), epochState, pendingOperations));
    }
  }

  private BeaconStateEx applyEpochTransitionIfNeeded(
      BeaconStateEx originalState, BeaconStateEx stateWithoutEpoch) {

    if (specHelpers.is_epoch_end(stateWithoutEpoch.getSlot())
        && originalState.getSlot().less(stateWithoutEpoch.getSlot())) {
      return perEpochTransition.apply(stateWithoutEpoch);
    } else {
      return null;
    }
  }

  /**
   * Applies next slot transitions until the <code>targetSlot</code> but
   * doesn't apply EpochTransition for the <code>targetSlot</code>
   *
   * @param source Source state
   * @return new state, result of applied transition to the latest input state
   */
  private BeaconStateEx applySlotTransitionsWithoutEpoch(
      BeaconStateEx source, SlotNumber targetSlot) {

    BeaconStateEx state = source;
    for (SlotNumber slot : source.getSlot().increment().iterateTo(targetSlot.increment())) {
      state = perSlotTransition.apply(state);
      if (specHelpers.is_epoch_end(slot) && !slot.equals(targetSlot)) {
        state = perEpochTransition.apply(state);
      }
    }
    return state;
  }

  private void updateHead(PendingOperations pendingOperations) {
    BeaconBlock newHead =
        headFunction.getHead(
            validatorRecord -> pendingOperations.getLatestAttestation(validatorRecord.getPubKey()));
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
