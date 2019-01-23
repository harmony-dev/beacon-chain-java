package org.ethereum.beacon.validator;

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
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/** Runs a single validator in the same instance with chain processing. */
public class BeaconChainValidator implements ValidatorService {

  private ValidatorCredentials credentials;
  private BeaconChainProposer proposer;
  private BeaconChainAttester attester;
  private SpecHelpers specHelpers;
  private MessageSigner<Bytes96> messageSigner;

  private ScheduledExecutorService executor;

  private ObservableBeaconState recentState;
  private Hash32 recentDepositRoot;
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
  }

  @Override
  public void stop() {
    this.executor.shutdown();
  }

  private void init(BeaconState state) {
    this.index = specHelpers.get_validator_index_by_pubkey(state, credentials.getBlsPublicKey());
  }

  private void processState(ObservableBeaconState state) {
    if (!specHelpers.is_current_slot(state.getLatestSlotState())) {
      return;
    }

    this.recentState = state;

    if (!isInitialized()) {
      init(state.getLatestSlotState());
    }

    if (isInitialized()) {
      runTasks(state);
    }
  }

  private void processDepositRoot(Hash32 depositRoot) {
    this.recentDepositRoot = depositRoot;
  }

  private void runTasks(ObservableBeaconState observableState) {
    BeaconState state = observableState.getLatestSlotState();
    UInt24 proposerIndex = specHelpers.get_beacon_proposer_index(state, state.getSlot());
    if (index.equals(proposerIndex)) {
      propose(observableState);
    } else if (specHelpers.is_in_beacon_chain_committee(state, state.getSlot(), index)) {
      attest(observableState);
    }
  }

  private void propose(final ObservableBeaconState observableState) {
    if (recentDepositRoot != null) {
      executor.execute(
          () -> {
            final BeaconBlock newBlock =
                proposer.propose(observableState, recentDepositRoot, messageSigner);
            propagateBlock(newBlock);
          });
    }
  }

  private void attest(final ObservableBeaconState observableState) {
    BeaconState state = observableState.getLatestSlotState();
    UInt64 slotMiddleTime = specHelpers.get_slot_middle_time(state, state.getSlot());
    long slotMiddleTimeMillis = 1000 * slotMiddleTime.getValue();

    executor.schedule(
        () -> {
          final Attestation attestation = attester.attest(this.recentState);
          propagateAttestation(attestation);
        },
        Math.max(0, System.currentTimeMillis() - slotMiddleTimeMillis),
        TimeUnit.MILLISECONDS);
  }

  private boolean isInitialized() {
    return index.compareTo(UInt24.MAX_VALUE) < 0;
  }

  /* FIXME: stub for streams. */
  private void propagateBlock(BeaconBlock newBlock) {}

  private void propagateAttestation(Attestation attestation) {}

  private void subscribeToObservableStateUpdates(Consumer<ObservableBeaconState> payload) {}

  private void subsribeToDepositRootUpdates(Consumer<Hash32> payload) {}
}
