package org.ethereum.beacon.test.runner.state;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.StateTransitions;
import org.ethereum.beacon.consensus.spec.SpecCommons;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.consensus.transition.ExtendedSlotTransition;
import org.ethereum.beacon.consensus.transition.PerBlockTransition;
import org.ethereum.beacon.consensus.transition.PerEpochTransition;
import org.ethereum.beacon.consensus.transition.PerSlotTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.consensus.verifier.operation.AttestationVerifier;
import org.ethereum.beacon.consensus.verifier.operation.AttesterSlashingVerifier;
import org.ethereum.beacon.consensus.verifier.operation.DepositVerifier;
import org.ethereum.beacon.consensus.verifier.operation.ProposerSlashingVerifier;
import org.ethereum.beacon.consensus.verifier.operation.TransferVerifier;
import org.ethereum.beacon.consensus.verifier.operation.VoluntaryExitVerifier;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.test.StateTestUtils;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.state.StateTestCase;
import org.ethereum.beacon.test.type.state.StateTestCase.BeaconStateData;
import org.javatuples.Pair;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * TestRunner for {@link StateTestCase}
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/operations">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/operations</a>
 */
public class StateRunner implements Runner {
  private StateTestCase testCase;
  private BeaconChainSpec spec;
  private String handler;

  public StateRunner(TestCase testCase, BeaconChainSpec spec, String handler) {
    if (!(testCase instanceof StateTestCase)) {
      throw new RuntimeException("TestCase runner accepts only StateTestCase.class as input!");
    }
    this.testCase = (StateTestCase) testCase;
    this.spec = spec;
    this.handler = handler;
  }

  public Optional<String> run() {
    BeaconState initialState = buildInitialState(spec, testCase.getPre());
    Optional<String> err = StateComparator.compare(testCase.getPre(), initialState, spec);
    if (err.isPresent()) {
      return Optional.of("Initial state parsed incorrectly: " + err.get());
    }
    BeaconState latestState = initialState;
    Optional<String> processingError;

    BeaconState stateBackup = latestState.createMutableCopy();
    switch (handler) {
      case "deposit":
        processingError = processDeposit(testCase.getDepositOperation(), latestState);
        break;
      case "attestation":
        processingError =
            processAttestation(testCase.getAttestationOperation(spec.getConstants()), latestState);
        break;
      case "attester_slashing":
        processingError =
            processAttesterSlashing(testCase.getAttesterSlashingOperation(), latestState);
        break;
      case "proposer_slashing":
        processingError =
            processProposerSlashing(testCase.getProposerSlashingOperation(), latestState);
        break;
      case "transfer":
        processingError = processTransfer(testCase.getTransferOperation(), latestState);
        break;
      case "voluntary_exit":
        processingError = processVoluntaryExit(testCase.getVoluntaryExitOperation(), latestState);
        break;
      case "block_header":
        processingError =
            processBlockHeader(testCase.getBeaconBlock(spec.getConstants()), latestState);
        break;
      case "crosslinks":
        processingError = processCrosslinks(latestState);
        break;
      case "registry_updates":
        processingError = processRegistryUpdates(latestState);
        break;
      case "final_updates":
        processingError = processFinalUpdates(latestState);
        break;
      case "justification_and_finalization":
        processingError = processJustificationAndFinalization(latestState);
        break;
      case "slashings":
        processingError = processSlashings(latestState);
        break;
      case "slots":
        Pair<Optional<String>, BeaconState> processingSlots =
            processSlots(testCase.getSlots(), latestState);
        processingError = processingSlots.getValue0();
        if (!processingError.isPresent()) {
          latestState = processingSlots.getValue1();
        }
        break;
      case "blocks":
        Pair<Optional<String>, BeaconState> processingBlocks =
            processBlocks(testCase.getBeaconBlocks(spec.getConstants()), latestState);
        processingError = processingBlocks.getValue0();
        if (!processingError.isPresent()) {
          latestState = processingBlocks.getValue1();
        }
        break;
      default:
        throw new RuntimeException("This type of state test is not supported");
    }
    if (processingError.isPresent()) {
      latestState = stateBackup;
    }

    if (testCase.getPost() == null) { // XXX: Not changed
      return StateComparator.compare(testCase.getPre(), latestState, spec);
    } else {
      Optional compareResult = StateComparator.compare(testCase.getPost(), latestState, spec);
      if (!compareResult.isPresent()) {
        return Optional.empty();
      }

      String processingErrorMessage = "Processing error: ";
      if (processingError.isPresent()) {
        processingErrorMessage += processingError.get();
      }
      return Optional.of(compareResult.get() + processingErrorMessage);
    }
  }

  private Optional<String> processDeposit(Deposit deposit, BeaconState state) {
    DepositVerifier depositVerifier = new DepositVerifier(spec);
    return processOperation(
        deposit,
        state,
        depositVerifier,
        objects ->
            spec.process_deposit((MutableBeaconState) objects.getValue1(), objects.getValue0()));
  }

  private Optional<String> processAttestation(Attestation attestation, BeaconState state) {
    AttestationVerifier attestationVerifier = new AttestationVerifier(spec);
    return processOperation(
        attestation,
        state,
        attestationVerifier,
        objects ->
            spec.process_attestation(
                (MutableBeaconState) objects.getValue1(), objects.getValue0()));
  }

  private Optional<String> processAttesterSlashing(
      AttesterSlashing attesterSlashing, BeaconState state) {
    AttesterSlashingVerifier slashingVerifier = new AttesterSlashingVerifier(spec);
    return processOperation(
        attesterSlashing,
        state,
        slashingVerifier,
        objects ->
            spec.process_attester_slashing(
                (MutableBeaconState) objects.getValue1(), objects.getValue0()));
  }

  private Optional<String> processProposerSlashing(
      ProposerSlashing proposerSlashing, BeaconState state) {
    ProposerSlashingVerifier slashingVerifier = new ProposerSlashingVerifier(spec);
    return processOperation(
        proposerSlashing,
        state,
        slashingVerifier,
        objects ->
            spec.process_proposer_slashing(
                (MutableBeaconState) objects.getValue1(), objects.getValue0()));
  }

  private Optional<String> processTransfer(Transfer transfer, BeaconState state) {
    TransferVerifier verifier = new TransferVerifier(spec);
    return processOperation(
        transfer,
        state,
        verifier,
        objects ->
            spec.process_transfer((MutableBeaconState) objects.getValue1(), objects.getValue0()));
  }

  private Optional<String> processVoluntaryExit(VoluntaryExit voluntaryExit, BeaconState state) {
    VoluntaryExitVerifier verifier = new VoluntaryExitVerifier(spec);
    return processOperation(
        voluntaryExit,
        state,
        verifier,
        objects ->
            spec.process_voluntary_exit(
                (MutableBeaconState) objects.getValue1(), objects.getValue0()));
  }

  private Optional<String> processBlockHeader(BeaconBlock block, BeaconState state) {
    BeaconBlockVerifier verifier = BeaconBlockVerifier.createDefault(spec);
    try {
      VerificationResult verificationResult = verifier.verify(block, state);
      if (verificationResult.isPassed()) {
        spec.process_block_header((MutableBeaconState) state, block);
        return Optional.empty();
      } else {
        return Optional.of(verificationResult.getMessage());
      }
    } catch (SpecCommons.SpecAssertionFailed | IllegalArgumentException ex) {
      return Optional.of(ex.getMessage());
    }
  }

  private Optional<String> processCrosslinks(BeaconState state) {
    try {
      spec.process_crosslinks((MutableBeaconState) state);
      return Optional.empty();
    } catch (SpecCommons.SpecAssertionFailed | IllegalArgumentException ex) {
      return Optional.of(ex.getMessage());
    }
  }

  private Optional<String> processRegistryUpdates(BeaconState state) {
    try {
      spec.process_registry_updates((MutableBeaconState) state);
      return Optional.empty();
    } catch (SpecCommons.SpecAssertionFailed | IllegalArgumentException ex) {
      return Optional.of(ex.getMessage());
    }
  }

  private Optional<String> processFinalUpdates(BeaconState state) {
    try {
      spec.process_final_updates((MutableBeaconState) state);
      return Optional.empty();
    } catch (SpecCommons.SpecAssertionFailed | IllegalArgumentException ex) {
      return Optional.of(ex.getMessage());
    }
  }

  private Optional<String> processJustificationAndFinalization(BeaconState state) {
    try {
      spec.process_justification_and_finalization((MutableBeaconState) state);
      return Optional.empty();
    } catch (SpecCommons.SpecAssertionFailed | IllegalArgumentException ex) {
      return Optional.of(ex.getMessage());
    }
  }

  private Optional<String> processSlashings(BeaconState state) {
    try {
      spec.process_slashings((MutableBeaconState) state);
      return Optional.empty();
    } catch (SpecCommons.SpecAssertionFailed | IllegalArgumentException ex) {
      return Optional.of(ex.getMessage());
    }
  }

  private Pair<Optional<String>, BeaconState> processSlots(int slots, BeaconState state) {
    StateTransition<BeaconStateEx> perEpochTransition = new PerEpochTransition(spec);
    StateTransition<BeaconStateEx> perSlotTransition = new PerSlotTransition(spec);
    EmptySlotTransition slotTransition =
        new EmptySlotTransition(
            new ExtendedSlotTransition(
                new PerEpochTransition(spec) {
                  @Override
                  public BeaconStateEx apply(BeaconStateEx stateEx) {
                    return perEpochTransition.apply(stateEx);
                  }
                },
                perSlotTransition,
                spec));

    BeaconStateEx stateEx = new BeaconStateExImpl(state);
    try {
      BeaconStateEx res = slotTransition.apply(stateEx, stateEx.getSlot().plus(slots));
      return Pair.with(Optional.empty(), res.createMutableCopy());
    } catch (SpecCommons.SpecAssertionFailed | IllegalArgumentException ex) {
      return Pair.with(Optional.of(ex.getMessage()), null);
    }
  }

  private Pair<Optional<String>, BeaconState> processBlocks(
      List<BeaconBlock> blocks, BeaconState state) {
    EmptySlotTransition preBlockTransition = StateTransitions.preBlockTransition(spec);
    PerBlockTransition blockTransition = StateTransitions.blockTransition(spec);
    BeaconBlockVerifier blockVerifier = BeaconBlockVerifier.createDefault(spec);
    BeaconStateVerifier stateVerifier = BeaconStateVerifier.createDefault(spec);
    BeaconStateEx stateEx = new BeaconStateExImpl(state);
    try {
      for (BeaconBlock block : blocks) {
        stateEx = preBlockTransition.apply(stateEx, block.getSlot());
        VerificationResult blockVerification = blockVerifier.verify(block, stateEx);
        if (!blockVerification.isPassed()) {
          return Pair.with(Optional.of("Invalid block"), null);
        }
        stateEx = blockTransition.apply(stateEx, block);
        VerificationResult stateVerification = stateVerifier.verify(stateEx, block);
        if (!stateVerification.isPassed()) {
          return Pair.with(Optional.of("State mismatch"), null);
        }
      }
      return Pair.with(Optional.empty(), stateEx.createMutableCopy());
    } catch (SpecCommons.SpecAssertionFailed | IllegalArgumentException ex) {
      return Pair.with(Optional.of(ex.getMessage()), null);
    }
  }

  private <O> Optional<String> processOperation(
      O operation,
      BeaconState state,
      OperationVerifier<O> verifier,
      Consumer<Pair<O, BeaconState>> operationProcessor) {
    try {
      VerificationResult verificationResult = verifier.verify(operation, state);
      if (verificationResult.isPassed()) {
        operationProcessor.accept(Pair.with(operation, state));
        return Optional.empty();
      } else {
        return Optional.of(verificationResult.getMessage());
      }
    } catch (SpecCommons.SpecAssertionFailed
        | IllegalArgumentException
        | IndexOutOfBoundsException ex) {
      return Optional.of(ex.getMessage());
    }
  }

  private BeaconState buildInitialState(BeaconChainSpec spec, BeaconStateData stateData) {
    return StateTestUtils.parseBeaconState(spec.getConstants(), stateData);
  }
}
