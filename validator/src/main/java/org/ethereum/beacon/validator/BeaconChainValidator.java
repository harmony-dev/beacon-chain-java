package org.ethereum.beacon.validator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;

/** Runs a single validator in the same instance with chain processing. */
public class BeaconChainValidator implements ValidatorService {

  private ValidatorCredentials credentials;
  private BeaconChainProposer proposer;
  private BeaconChainAttester attester;
  private SpecHelpers specHelpers;
  private MessageSigner<Bytes96> messageSigner;

  private ScheduledExecutorService executor;

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
      runAsync(() -> propose(observableState));
    } else if (specHelpers.is_in_beacon_chain_committee(state, state.getSlot(), index)) {
      runAsync(() -> attest(observableState));
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

  private void subscribeToObservableStateUpdates(Consumer<ObservableBeaconState> payload) {}
}
