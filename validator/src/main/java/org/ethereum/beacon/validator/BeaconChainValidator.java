package org.ethereum.beacon.validator;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.types.SlotTick;
import org.ethereum.beacon.types.ValidatorEvent;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;

/** Runs a single validator in the same instance with chain processing. */
public class BeaconChainValidator implements ValidatorService {

  private static int DELAY_MILLIS_AFTER_TICK = 1000;

  private ValidatorCredentials credentials;
  private BeaconChainProposer proposer;
  private BeaconChainAttester attester;
  private SpecHelpers specHelpers;
  private MessageSigner<Bytes96> messageSigner;

  private ScheduledExecutorService executor;

  private Publisher<ObservableBeaconState> observableBeaconStatePublisher;
  private Disposable observableStateSubscription = null;
  private Publisher<SlotTick> slotTickPublisher;
  private Disposable slotTickSubscription = null;
  private final ReplayProcessor<ValidatorEvent> validatorEventSink = ReplayProcessor.cacheLast();
  private final Publisher<ValidatorEvent> validatorEventStream = Flux.from(validatorEventSink)
      .publishOn(Schedulers.single())
      .onBackpressureError()
      .name("BeaconChainValidator.validatorEvent");

  private Flux<Runnable> currentTask = null;

  private UInt24 index = UInt24.MAX_VALUE;

  public BeaconChainValidator(
      ValidatorCredentials credentials,
      BeaconChainProposer proposer,
      BeaconChainAttester attester,
      SpecHelpers specHelpers,
      MessageSigner<Bytes96> messageSigner) {
    this.credentials = credentials;
    this.proposer = proposer;
    this.attester = attester;
    this.specHelpers = specHelpers;
    this.messageSigner = messageSigner;
  }

  @Override
  public void start() {
    this.executor =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread t = new Thread(runnable, "validator-service");
              t.setDaemon(true);
              return t;
            });
    subscribeToObservableStateUpdates(this::processState);
    subscribeToSlotTickUpdates(this::onSlotTick);
  }

  @Override
  public void stop() {
    this.observableStateSubscription.dispose();;
    this.slotTickSubscription.dispose();
    this.executor.shutdown();
  }

  private void init(BeaconState state) {
    this.index = specHelpers.get_validator_index_by_pubkey(state, credentials.getBlsPublicKey());
  }

  private void onSlotTick(SlotTick newTick) {
    if (isInitialized()) {
      validatorEventSink.onNext(new ValidatorEvent());
      if (currentTask != null) {
        currentTask
            .delaySubscription(Duration.ofSeconds(DELAY_MILLIS_AFTER_TICK))
            .next()
            .subscribe(this::runAsync);
      }
    }
  }

  private void processState(ObservableBeaconState state) {
    if (!specHelpers.is_current_slot(state.getLatestSlotState())) {
      return;
    }

    if (!isInitialized()) {
      init(state.getLatestSlotState());
    }

    if (isInitialized()) {
      runTasks(state);
    }
  }

  private void runTasks(final ObservableBeaconState observableState) {
    BeaconState state = observableState.getLatestSlotState();
    UInt24 proposerIndex = specHelpers.get_beacon_proposer_index(state, state.getSlot());
    if (index.equals(proposerIndex)) {
      createNewTask(() -> propose(observableState));
    } else if (specHelpers.is_in_beacon_chain_committee(state, state.getSlot(), index)) {
      createNewTask(() -> attest(observableState));
    }
  }

  private void createNewTask(Runnable routine) {
    if (currentTask == null) {
      currentTask = Flux.just(routine).cache(1);
    } else {
      currentTask = currentTask.mergeWith(Flux.just(routine));
    }
  }

  private void runAsync(Runnable routine) {
    executor.execute(routine);
  }

  private void propose(final ObservableBeaconState observableState) {
    BeaconBlock newBlock = proposer.propose(index, observableState, messageSigner);
    propagateBlock(newBlock);
  }

  private void attest(final ObservableBeaconState observableState) {
    Attestation attestation = attester.attest(observableState);
    propagateAttestation(attestation);
  }

  private boolean isInitialized() {
    return index.compareTo(UInt24.MAX_VALUE) < 0;
  }

  /* FIXME: stub for streams. */
  private void propagateBlock(BeaconBlock newBlock) {}

  private void propagateAttestation(Attestation attestation) {}

  private void subscribeToObservableStateUpdates(Consumer<ObservableBeaconState> payload) {
    this.observableStateSubscription = Flux.from(observableBeaconStatePublisher)
        .doOnNext(payload)
        .subscribe();
  }

  private void subscribeToSlotTickUpdates(Consumer<SlotTick> payload) {
    this.slotTickSubscription = Flux.from(slotTickPublisher)
        .doOnNext(payload)
        .subscribe();
  }
}
