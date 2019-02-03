package org.ethereum.beacon.chain.observer;

import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.LMDGhostHeadFunction;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.HeadFunction;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObservableStateProcessorImpl implements ObservableStateProcessor {

  private BeaconTupleStorage tupleStorage;
  private ObservableBeaconState observableState;
  private BeaconChainHead head;
  private HeadFunction headFunction;

  private BeaconState latestState;
  private SpecHelpers specHelpers;
  private ChainSpec chainSpec;

  private static final int UPDATE_MILLIS = 500;
  private Flux updateIntervals = Flux.interval(Duration.ofMillis(UPDATE_MILLIS));
  private final Map<Bytes48, Attestation> attestationCache = new HashMap<>();
  private final Map<UInt64, Set<Bytes48>> validatorSlotCache = new HashMap<>();

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
      Publisher<BeaconState> slotStatesStream,
      Publisher<Attestation> attestationPublisher,
      Publisher<PendingAttestationRecord> pendingAttestationRecordPublisher,
      SpecHelpers specHelpers) {
    this.tupleStorage = chainStorage.getBeaconTupleStorage();
    this.specHelpers = specHelpers;
    this.chainSpec = specHelpers.getChainSpec();
    this.headFunction = new LMDGhostHeadFunction(chainStorage, specHelpers);
    Flux.from(slotStatesStream).doOnNext(this::onSlotStateUpdate).subscribe();
    Flux.from(attestationPublisher).doOnNext(this::onNewAttestation).subscribe();
    Flux.from(pendingAttestationRecordPublisher)
        .doOnNext(this::onNewPendingAttestation)
        .subscribe();
    updateIntervals.subscribe(tick -> updateCurrentObservableState());
  }

  private void onSlotStateUpdate(BeaconState slotState) {
    this.latestState = slotState;
    // From spec: Verify that attestation.data.slot <= state.slot - MIN_ATTESTATION_INCLUSION_DELAY
    // < attestation.data.slot + EPOCH_LENGTH
    // state.slot - MIN_ATTESTATION_INCLUSION_DELAY < attestation.data.slot + EPOCH_LENGTH
    // state.slot - MIN_ATTESTATION_INCLUSION_DELAY - EPOCH_LENGTH < attestation.data.slot
    purgeAttestations(
        latestState
            .getSlot()
            .minus(chainSpec.getEpochLength())
            .minus(chainSpec.getMinAttestationInclusionDelay()));
    updateCurrentObservableState();
  }

  private void onNewAttestation(Attestation attestation) {
    List<UInt24> participants =
        specHelpers.get_attestation_participants(
            latestState, attestation.getData(), attestation.getParticipationBitfield());

    List<Bytes48> pubKeys = specHelpers.mapIndicesToPubKeys(latestState, participants);

    for (Bytes48 pubKey : pubKeys) {
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
  }

  private void addToSlotCache(UInt64 slot, Bytes48 pubKey) {
    if (validatorSlotCache.containsKey(slot)) {
      validatorSlotCache.get(slot).add(pubKey);
    } else {
      Set<Bytes48> pubKeysSet = new HashSet<>();
      pubKeysSet.add(pubKey);
      validatorSlotCache.put(slot, pubKeysSet);
    }
  }

  private void onNewPendingAttestation(PendingAttestationRecord pendingAttestationRecord) {
    List<UInt24> participants =
        specHelpers.get_attestation_participants(
            latestState,
            pendingAttestationRecord.getData(),
            pendingAttestationRecord.getParticipationBitfield());
    List<Bytes48> pubKeys = specHelpers.mapIndicesToPubKeys(latestState, participants);
    UInt64 slot = pendingAttestationRecord.getData().getSlot();
    pubKeys.forEach(
        pubkey -> {
          attestationCache.remove(pubkey);
          if (validatorSlotCache.containsKey(slot)) {
            validatorSlotCache.get(slot).remove(pubkey);
          }
        });
  }

  /** Purges all entries for slot and before */
  private void purgeAttestations(UInt64 slot) {
    for (Map.Entry<UInt64, Set<Bytes48>> entry : validatorSlotCache.entrySet()) {
      if (entry.getKey().compareTo(slot) <= 0) {
        entry.getValue().forEach(attestationCache::remove);
        validatorSlotCache.remove(entry.getKey());
      }
    }
  }

  private void updateCurrentObservableState() {
    PendingOperations pendingOperations = new PendingOperationsState(attestationCache);
    pendingOperationsSink.onNext(pendingOperations);
    updateHead(pendingOperations);
    ObservableBeaconState newObservableState =
        new ObservableBeaconState(head.getBlock(), latestState, pendingOperations);
    if (!newObservableState.equals(observableState)) {
      this.observableState = newObservableState;
      observableStateSink.onNext(newObservableState);
    }
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
            .get(newHead.getHash())
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
