package org.ethereum.beacon.validator;

import java.util.function.Consumer;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.db.Database;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/** Runs a single validator in the same instance with chain processing. */
public class BeaconChainValidator implements ValidatorService {

  private static final String RANDAO_SOURCE = "validator_randao_source";

  private ValidatorCredentials credentials;
  private Database database;
  private ValidatorTaskManager taskManager;
  private BeaconChainProposer proposer;
  private BeaconChainAttester attester;
  private MutableBeaconChain beaconChain;
  private SpecHelpers specHelpers;

  private ObservableBeaconState recentState;
  private UInt24 index = UInt24.MAX_VALUE;

  public BeaconChainValidator(
      ValidatorCredentials credentials,
      Database database,
      ValidatorTaskManager taskManager,
      BeaconChainProposer proposer,
      BeaconChainAttester attester,
      MutableBeaconChain beaconChain,
      SpecHelpers specHelpers) {
    this.credentials = credentials;
    this.database = database;
    this.taskManager = taskManager;
    this.proposer = proposer;
    this.attester = attester;
    this.beaconChain = beaconChain;
    this.specHelpers = specHelpers;
  }

  @Override
  public void start() {
    subscribeToObservableStateUpdates(this::processState);
  }

  @Override
  public void stop() {
    this.taskManager.stop();
  }

  private void init(BeaconState state) {
    if (isInitialized()) {
      return;
    }

    UInt24 index = specHelpers.get_validator_index_by_pubkey(state, credentials.getBlsPublicKey());

    if (index.compareTo(UInt24.MAX_VALUE) < 0) {
      this.index = index;
      this.taskManager.start();
      createTasks(state);
      subscribeToEpochUpdates(this::createTasks);
    }
  }

  private void createTasks(BeaconState state) {
    if (isInitialized()) {
      UInt64 assignedSlot = specHelpers.get_assigned_slot(state, index);
      if (!assignedSlot.equals(UInt64.MAX_VALUE) && assignedSlot.compareTo(state.getSlot()) >= 0) {
        if (index.equals(specHelpers.get_beacon_proposer_index(state, assignedSlot))) {
          proposeAt(assignedSlot);
        } else {
          attestAt(assignedSlot);
        }
      }
    }
  }

  private void processState(ObservableBeaconState state) {
    this.recentState = state;
    if (!isInitialized() && specHelpers.is_current_slot(state.getLatestSlotState())) {
      init(state.getLatestSlotState());
    }
  }

  private void proposeAt(UInt64 slot) {
    taskManager.scheduleAtStart(
        slot,
        index,
        () -> {
          final ObservableBeaconState state = fetchState(slot.decrement());
          final BeaconBlock newBlock = proposer.propose(state);
          propagateBlock(newBlock);
        });
  }

  private void attestAt(UInt64 slot) {
    taskManager.scheduleInTheMiddle(
        slot,
        index,
        () -> {
          final ObservableBeaconState state = fetchState(slot);
          final Attestation attestation = attester.attest(state);
          propagateAttestation(attestation);
        });
  }

  private boolean isInitialized() {
    return index.compareTo(UInt24.MAX_VALUE) < 0;
  }

  private void propagateBlock(BeaconBlock newBlock) {
    beaconChain.insert(newBlock);
  }

  /* FIXME: update pending operations */
  private void propagateAttestation(Attestation attestation) {}

  /* FIXME: implement */
  private ObservableBeaconState fetchState(UInt64 slot) {
    return null;
  }

  /* FIXME: stub for subscriptions. */
  private void subscribeToEpochUpdates(Consumer<BeaconState> payload) {}

  private void subscribeToObservableStateUpdates(Consumer<ObservableBeaconState> payload) {}

  private void onPoWChainSyncDone(Runnable routine) {}
}
