package org.ethereum.beacon.validator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.schedulers.LatestExecutor;
import org.ethereum.beacon.schedulers.RunnableEx;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;

/** Runs several validators in one instance. */
public class MultiValidatorService implements ValidatorService {

  private static final Logger logger = LogManager.getLogger(MultiValidatorService.class);

  private final Schedulers schedulers;

  private final DirectProcessor<BeaconBlock> blocksSink = DirectProcessor.create();
  private final Publisher<BeaconBlock> blocksStream;

  private final DirectProcessor<Attestation> attestationsSink = DirectProcessor.create();
  private final Publisher<Attestation> attestationsStream;

  /** Proposer logic. */
  private BeaconChainProposer proposer;
  /** Attester logic. */
  private BeaconChainAttester attester;
  /** The spec. */
  private SpecHelpers specHelpers;

  private Publisher<ObservableBeaconState> stateStream;

  /** Credentials of yet not initialized validators. */
  private Map<BLSPubkey, BLS381Credentials> uninitialized;
  /** Credentials of already initialized validators. */
  private Map<ValidatorIndex, BLS381Credentials> initialized = new ConcurrentHashMap<>();
  /** Latest slot that has been processed. */
  private SlotNumber lastProcessedSlot = SlotNumber.ZERO;
  /** The most recent beacon state came from the outside. */
  private ObservableBeaconState recentState;

  /** Validator task executor. */
  private final Scheduler executor;

  private final LatestExecutor<BeaconState> initExecutor;

  public MultiValidatorService(
      List<BLS381Credentials> blsCredentials,
      BeaconChainProposer proposer,
      BeaconChainAttester attester,
      SpecHelpers specHelpers,
      Publisher<ObservableBeaconState> stateStream,
      Schedulers schedulers) {
    this.uninitialized =
        blsCredentials.stream()
            .collect(Collectors.toMap(BLS381Credentials::getPubkey, Function.identity()));
    this.proposer = proposer;
    this.attester = attester;
    this.specHelpers = specHelpers;
    this.stateStream = stateStream;
    this.schedulers = schedulers;

    blocksStream =
        Flux.from(blocksSink)
            .publishOn(this.schedulers.reactorEvents())
            .onBackpressureError()
            .name("BeaconChainValidator.block");

    attestationsStream =
        Flux.from(attestationsSink)
            .publishOn(this.schedulers.reactorEvents())
            .onBackpressureError()
            .name("BeaconChainValidator.attestation");

    executor = this.schedulers.newSingleThreadDaemon("validator-service");
    initExecutor = new LatestExecutor<>(schedulers.cpuHeavy(), this::initFromLatestBeaconState);
  }

  @Override
  public void start() {
    subscribeToStateUpdates(this::onNewState);
  }

  @Override
  public void stop() {}

  /**
   * Passes validators from {@link #uninitialized} map through initialization process.
   *
   * @param state a state object to seek for validators in.
   */
  private void initFromLatestBeaconState(BeaconState state) {
    Map<ValidatorIndex, BLS381Credentials> intoCommittees = new HashMap<>();
    for (ValidatorIndex i : state.getValidatorRegistry().size()) {
      BLS381Credentials credentials =
          uninitialized.remove(state.getValidatorRegistry().get(i).getPubKey());
      if (credentials != null) {
        intoCommittees.put(i, credentials);
      }
    }
    this.initialized.putAll(intoCommittees);

    if (!intoCommittees.isEmpty())
      logger.info("initialized validators: {}", intoCommittees.keySet());
  }

  /**
   * Keeps the most recent state in memory.
   *
   * <p>Recent state is required by delayed tasks like {@link #attest(ValidatorIndex)}.
   *
   * @param state state came from the outside.
   */
  private void keepRecentState(ObservableBeaconState state) {
    this.recentState = state;
  }

  /**
   * Connects outer state updates with validator behaviour. Is triggered on each new state received
   * from the outside.
   *
   * <p>It is assumed that outer component, that validator is subscribed to, sends a new state on
   * every processed and on every empty slot transition made on top of the chain head. * *
   *
   * <p><strong>Note:</strong> coming state is discarded if it isn't related to current slot.
   *
   * @param observableState a new state object.
   */
  @VisibleForTesting
  void onNewState(ObservableBeaconState observableState) {
    if (isCurrentSlot(observableState.getLatestSlotState())) {
      keepRecentState(observableState);
      processState(observableState);
    }
  }

  /**
   * Processes a new state.
   *
   * <p>Initializes service if it's possible and then runs validator tasks by calling to {@link
   * #runTasks(ObservableBeaconState)}.
   *
   * @param observableState a state object to process.
   */
  private void processState(ObservableBeaconState observableState) {
    BeaconState state = observableState.getLatestSlotState();

    if (!isSlotProcessed(state)) {
      setSlotProcessed(state);
      if (!isInitialized()) {
        initExecutor.newEvent(state);
      }
      runTasks(observableState);
    }
  }

  /**
   * Checks if any of {@link #initialized} validators are assigned to either propose or attest to a
   * block with slot equal to {@code observableState.slot}. And triggers corresponding routine:
   *
   * <ul>
   *   <li>{@link #propose(ValidatorIndex, ObservableBeaconState)} routine is triggered instantly
   *       with received {@code observableState} object.
   *   <li>{@link #attest(ValidatorIndex)} routine is a delayed task, it's called with {@link
   *       #recentState} object.
   * </ul>
   *
   * @param observableState a state that validator tasks are executed with.
   */
  @VisibleForTesting
  void runTasks(final ObservableBeaconState observableState) {
    BeaconState state = observableState.getLatestSlotState();

    // trigger proposer
    ValidatorIndex proposerIndex = specHelpers.get_beacon_proposer_index(state, state.getSlot());
    if (initialized.containsKey(proposerIndex)) {
      runAsync(() -> propose(proposerIndex, observableState));
    }

    // trigger attester at a halfway through the slot
    Time startAt = specHelpers.get_slot_middle_time(state, state.getSlot());
    List<ShardCommittee> committees =
        specHelpers.get_crosslink_committees_at_slot(state, state.getSlot());
    for (ShardCommittee sc : committees) {
      sc.getCommittee().stream()
          .filter(initialized::containsKey)
          .forEach(index -> schedule(startAt, () -> this.attest(index)));
    }
  }

  /**
   * Runs a routine asynchronously using {@link #executor}.
   *
   * @param routine a routine.
   */
  private void runAsync(RunnableEx routine) {
    executor.execute(routine);
  }

  /**
   * Schedules a routine for a point in the future.
   *
   * @param startAt a unix timestamp of start point, in seconds.
   * @param routine a routine.
   */
  private void schedule(Time startAt, RunnableEx routine) {
    long startAtMillis = startAt.getValue() * 1000;
    assert schedulers.getCurrentTime() < startAtMillis;
    executor.executeWithDelay(
        Duration.ofMillis(schedulers.getCurrentTime() - startAtMillis), routine);
  }

  /**
   * Proposes a new block that is build on top of given state.
   *
   * @param index index of proposer.
   * @param observableState a state.
   */
  private void propose(ValidatorIndex index, final ObservableBeaconState observableState) {
    BLS381Credentials credentials = initialized.get(index);
    if (credentials != null) {
      BeaconBlock newBlock = proposer.propose(observableState, credentials.getSigner());
      propagateBlock(newBlock);

      logger.info(
          "validator {}: proposed a {}",
          index,
          newBlock.toString(
              specHelpers.getChainSpec(),
              observableState.getLatestSlotState().getGenesisTime(),
              specHelpers::hash_tree_root));
    }
  }

  /**
   * Attests to a head block of {@link #recentState}.
   *
   * <p><strong>Note:</strong> since {@link #recentState} may be updated after attestation has been
   * scheduled, there is a sanity check that validator is still eligible to attest with {@link
   * #recentState}.
   *
   * @param index index of attester.
   */
  private void attest(ValidatorIndex index) {
    final ObservableBeaconState observableState = this.recentState;
    final BeaconState state = observableState.getLatestSlotState();

    Optional<ShardCommittee> validatorCommittee = getValidatorCommittee(index, state);
    BLS381Credentials credentials = initialized.get(index);
    if (validatorCommittee.isPresent() && credentials != null) {
      Attestation attestation =
          attester.attest(
              index, validatorCommittee.get().getShard(), observableState, credentials.getSigner());
      propagateAttestation(attestation);

      logger.info(
          "validator {}: attested to head: {} in a slot: {}",
          index,
          observableState
              .getHead()
              .toString(
                  specHelpers.getChainSpec(),
                  observableState.getLatestSlotState().getGenesisTime(),
                  specHelpers::hash_tree_root),
          state.getSlot());
    }
  }

  /**
   * Marks a slot of the state as a processed one.
   *
   * <p>Used by {@link #isSlotProcessed(BeaconState)} as a part of re-play protection.
   *
   * @param state a state.
   */
  private void setSlotProcessed(BeaconState state) {
    this.lastProcessedSlot = state.getSlot();
  }

  /** Returns committee where the validator participates if any */
  private Optional<ShardCommittee> getValidatorCommittee(ValidatorIndex index, BeaconState state) {
    List<ShardCommittee> committees =
        specHelpers.get_crosslink_committees_at_slot(state, state.getSlot());
    return committees.stream().filter(sc -> sc.getCommittee().contains(index)).findFirst();
  }

  /**
   * Checks whether slot of the state was already processed.
   *
   * <p>Processed slot means that tasks for this particular slot has already been initiated. Calling
   * to this method protects from slashing condition violation when tasks for same slot could be
   * initiated more than once due to chain re-orgs.
   *
   * @param state a state.
   * @return {@code true} if slot has been processed, {@link false} otherwise.
   */
  private boolean isSlotProcessed(BeaconState state) {
    return state.getSlot().lessEqual(lastProcessedSlot);
  }

  /**
   * Whether current moment in time belongs to a slot of the state.
   *
   * @param state a state.
   * @return {@link true} if current moment belongs to a slot, {@link false} otherwise.
   */
  private boolean isCurrentSlot(BeaconState state) {
    return specHelpers.is_current_slot(state);
  }

  /**
   * Whether validator's index has already been found in the recently processed state.
   *
   * @return {@code true} if index is defined, {@code false} otherwise.
   */
  private boolean isInitialized() {
    return uninitialized.isEmpty();
  }

  private void propagateBlock(BeaconBlock newBlock) {
    blocksSink.onNext(newBlock);
  }

  private void propagateAttestation(Attestation attestation) {
    attestationsSink.onNext(attestation);
  }

  private void subscribeToStateUpdates(Consumer<ObservableBeaconState> payload) {
    Flux.from(stateStream).subscribe(payload);
  }

  @VisibleForTesting
  ObservableBeaconState getRecentState() {
    return recentState;
  }

  @Override
  public Publisher<BeaconBlock> getProposedBlocksStream() {
    return blocksStream;
  }

  @Override
  public Publisher<Attestation> getAttestationsStream() {
    return attestationsStream;
  }
}
