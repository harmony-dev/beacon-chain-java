package org.ethereum.beacon.validator;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * A simple implementation of beacon chain validator.
 *
 * <p>Drives an "honest" validator instance alongside with chain processing.
 *
 * @see ValidatorService
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/validator/0_beacon-chain-validator.md">Honest
 *     validator</a> in the spec.
 */
public class BeaconChainValidator implements ValidatorService {

  /** BLS public key that corresponds to a "hot" private key. */
  private Bytes48 publicKey;
  /** Proposer logic. */
  private BeaconChainProposer proposer;
  /** Attester logic. */
  private BeaconChainAttester attester;
  /** The spec. */
  private SpecHelpers specHelpers;
  /** Helper that signs validator messages with a "hot" private key. */
  private MessageSigner<Bytes96> messageSigner;

  /** Validator index. Assigned in {@link #init(BeaconState)} method. */
  private UInt24 validatorIndex = UInt24.MAX_VALUE;
  /** Latest slot that has been processed. Initialized in {@link #init(BeaconState)} method. */
  private UInt64 lastProcessedSlot = UInt64.MAX_VALUE;
  /** The most recent beacon state came from the outside. */
  private ObservableBeaconState recentState;

  /** Validator task executor. */
  private ScheduledExecutorService executor;

  public BeaconChainValidator(
      Bytes48 publicKey,
      BeaconChainProposer proposer,
      BeaconChainAttester attester,
      SpecHelpers specHelpers,
      MessageSigner<Bytes96> messageSigner) {
    this.publicKey = publicKey;
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
    subscribeToStateUpdates(this::onNewState);
  }

  @Override
  public void stop() {
    this.executor.shutdown();
  }

  /**
   * Initializes validator by looking its index in validator registry.
   *
   * @param state a state object to seek for the validator in.
   */
  private void init(BeaconState state) {
    this.validatorIndex = specHelpers.get_validator_index_by_pubkey(state, publicKey);
    setSlotProcessed(state);
  }

  /**
   * Keeps the most recent state in memory.
   *
   * <p>Recent state is required by delayed tasks like {@link #attest()}.
   *
   * <p><strong>Note:</strong> coming state is discarded it isn't related to current slot.
   *
   * @param state state came from the outside.
   */
  private void keepRecentState(ObservableBeaconState state) {
    if (specHelpers.is_current_slot(state.getLatestSlotState())) {
      this.recentState = state;
    }
  }

  /**
   * Connects outer state updates with validator behaviour. Is triggered on each new state received
   * from the outside.
   *
   * <p>It is assumed that outer component, that validator is subscribed to, sends a new state on
   * every processed and on every empty slot transition made on top of the chain head.
   *
   * @param observableState a new state object.
   */
  private void onNewState(ObservableBeaconState observableState) {
    keepRecentState(observableState);
    BeaconState state = observableState.getLatestSlotState();

    if (!isInitialized() && isCurrentSlot(state)) {
      init(state);
    }

    if (isInitialized() && !isSlotProcessed(state)) {
      setSlotProcessed(state);
      runTasks(observableState);
    }
  }

  /**
   * Checks if validator is assigned to either propose or attest to a block with slot equal to
   * {@code observableState.slot}. And triggers corresponding routine:
   *
   * <ul>
   *   <li>{@link #propose(ObservableBeaconState)} routine is triggered instantly with received
   *       {@code observableState} object.
   *   <li>{@link #attest()} routine is a delayed task, it's called with {@link #recentState}
   *       object.
   * </ul>
   *
   * @param observableState a state that validator tasks are executed with.
   */
  private void runTasks(final ObservableBeaconState observableState) {
    BeaconState state = observableState.getLatestSlotState();

    // trigger proposer
    if (isEligibleToPropose(state)) {
      runAsync(() -> propose(observableState));
    }

    // trigger attester at a halfway through the slot
    if (isEligibleToAttest(state)) {
      UInt64 startAt = specHelpers.get_slot_middle_time(state, state.getSlot());
      schedule(startAt, this::attest);
    }
  }

  /**
   * Runs a routine asynchronously using {@link #executor}.
   *
   * @param routine a routine.
   */
  private void runAsync(Runnable routine) {
    executor.execute(routine);
  }

  /**
   * Schedules a routine for a point in the future.
   *
   * @param startAt a unix timestamp of start point, in seconds.
   * @param routine a routine.
   */
  private void schedule(UInt64 startAt, Runnable routine) {
    long startAtMillis = startAt.getValue() * 1000;
    assert System.currentTimeMillis() < startAtMillis;
    executor.schedule(routine, System.currentTimeMillis() - startAtMillis, TimeUnit.MILLISECONDS);
  }

  /**
   * Proposes a new block that is build on top of given state.
   *
   * @param observableState a state.
   */
  private void propose(final ObservableBeaconState observableState) {
    BeaconBlock newBlock = proposer.propose(observableState, messageSigner);
    propagateBlock(newBlock);
  }

  /**
   * Attests to a head block of {@link #recentState}.
   *
   * <p><strong>Note:</strong> since {@link #recentState} may be updated after attestation has been
   * scheduled, there is a sanity check that validator is still eligible to attest with {@link
   * #recentState}.
   */
  private void attest() {
    final ObservableBeaconState observableState = this.recentState;
    final BeaconState state = observableState.getLatestSlotState();

    if (isEligibleToAttest(state)) {
      Attestation attestation =
          attester.attest(
              validatorIndex,
              specHelpers.getChainSpec().getBeaconChainShardNumber(),
              observableState,
              messageSigner);
      propagateAttestation(attestation);
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

  /**
   * Whether validator is assigned to propose a block at a slot of the state.
   *
   * @param state a state.
   * @return {@code true} if assigned, {@link false} otherwise.
   */
  private boolean isEligibleToPropose(BeaconState state) {
    return validatorIndex.equals(specHelpers.get_beacon_proposer_index(state, state.getSlot()));
  }

  /**
   * Whether validator is assigned to attest to a head at a slot of the state.
   *
   * @param state a state.
   * @return {@code true} if assigned, {@link false} otherwise.
   */
  private boolean isEligibleToAttest(BeaconState state) {
    final List<UInt24> firstCommittee =
        specHelpers.get_shard_committees_at_slot(state, state.getSlot()).get(0).getCommittee();
    return Collections.binarySearch(firstCommittee, validatorIndex) >= 0;
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
    return lastProcessedSlot.compareTo(state.getSlot()) < 0;
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
    return validatorIndex.compareTo(UInt24.MAX_VALUE) < 0;
  }

  /* FIXME: stub for streams. */
  private void propagateBlock(BeaconBlock newBlock) {}

  private void propagateAttestation(Attestation attestation) {}

  private void subscribeToStateUpdates(Consumer<ObservableBeaconState> payload) {}
}
