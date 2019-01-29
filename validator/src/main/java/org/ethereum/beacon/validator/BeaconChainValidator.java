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

  private Bytes48 publicKey;
  private BeaconChainProposer proposer;
  private BeaconChainAttester attester;
  private SpecHelpers specHelpers;
  private MessageSigner<Bytes96> messageSigner;

  private ScheduledExecutorService executor;

  private UInt24 validatorIndex = UInt24.MAX_VALUE;
  private UInt64 lastProcessedSlot = UInt64.MAX_VALUE;

  private ObservableBeaconState recentState;

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

  private void init(BeaconState state) {
    this.validatorIndex = specHelpers.get_validator_index_by_pubkey(state, publicKey);
    setSlotProcessed(state);
  }

  private void keepRecentState(ObservableBeaconState state) {
    if (specHelpers.is_current_slot(state.getLatestSlotState())) {
      this.recentState = state;
    }
  }

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

  private void runAsync(Runnable routine) {
    executor.execute(routine);
  }

  private void schedule(UInt64 startAt, Runnable routine) {
    long startAtMillis = startAt.getValue() * 1000;
    assert System.currentTimeMillis() < startAtMillis;
    executor.schedule(routine, System.currentTimeMillis() - startAtMillis, TimeUnit.MILLISECONDS);
  }

  private void propose(final ObservableBeaconState observableState) {
    BeaconBlock newBlock = proposer.propose(validatorIndex, observableState, messageSigner);
    propagateBlock(newBlock);
  }

  private void attest() {
    final ObservableBeaconState observableState = this.recentState;
    final BeaconState state = observableState.getLatestSlotState();

    if (isEligibleToAttest(state)) {
      final List<UInt24> firstCommittee =
          specHelpers.get_shard_committees_at_slot(state, state.getSlot()).get(0).getCommittee();
      Attestation attestation =
          attester.attest(
              validatorIndex,
              firstCommittee,
              specHelpers.getChainSpec().getBeaconChainShardNumber(),
              observableState,
              messageSigner);
      propagateAttestation(attestation);
    }
  }

  private void setSlotProcessed(BeaconState state) {
    this.lastProcessedSlot = state.getSlot();
  }

  private boolean isEligibleToPropose(BeaconState state) {
    return validatorIndex.equals(specHelpers.get_beacon_proposer_index(state, state.getSlot()));
  }

  private boolean isEligibleToAttest(BeaconState state) {
    final List<UInt24> firstCommittee =
        specHelpers.get_shard_committees_at_slot(state, state.getSlot()).get(0).getCommittee();
    return Collections.binarySearch(firstCommittee, validatorIndex) >= 0;
  }

  private boolean isSlotProcessed(BeaconState state) {
    return lastProcessedSlot.compareTo(state.getSlot()) < 0;
  }

  private boolean isCurrentSlot(BeaconState state) {
    return specHelpers.is_current_slot(state);
  }

  private boolean isInitialized() {
    return validatorIndex.compareTo(UInt24.MAX_VALUE) < 0;
  }

  /* FIXME: stub for streams. */
  private void propagateBlock(BeaconBlock newBlock) {}

  private void propagateAttestation(Attestation attestation) {}

  private void subscribeToStateUpdates(Consumer<ObservableBeaconState> payload) {}
}
