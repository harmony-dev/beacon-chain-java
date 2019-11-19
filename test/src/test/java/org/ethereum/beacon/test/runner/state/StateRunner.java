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
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.state.CrosslinksProcessingCase;
import org.ethereum.beacon.test.type.state.FinalUpdatesProcessingCase;
import org.ethereum.beacon.test.type.state.FinalizationProcessingCase;
import org.ethereum.beacon.test.type.state.OperationAttestationCase;
import org.ethereum.beacon.test.type.state.OperationAttesterSlashingCase;
import org.ethereum.beacon.test.type.state.OperationBlockHeaderCase;
import org.ethereum.beacon.test.type.state.OperationDepositCase;
import org.ethereum.beacon.test.type.state.OperationProposerSlashingCase;
import org.ethereum.beacon.test.type.state.OperationTransferCase;
import org.ethereum.beacon.test.type.state.OperationVoluntaryExitCase;
import org.ethereum.beacon.test.type.state.RegistryUpdatesProcessingCase;
import org.ethereum.beacon.test.type.state.SanityBlocksCase;
import org.ethereum.beacon.test.type.state.SanitySlotsCase;
import org.ethereum.beacon.test.type.state.SlashingsProcessingCase;
import org.ethereum.beacon.test.type.state.field.PostField;
import org.ethereum.beacon.test.type.state.field.PreField;
import org.javatuples.Pair;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * TestRunner for several types of state tests including operations processing and epoch processing
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/operations">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/operations</a>
 */
public class StateRunner implements Runner {
  private TestCase testCase;
  private BeaconChainSpec spec;

  public StateRunner(TestCase testCase, BeaconChainSpec spec) {
    this.testCase = testCase;
    this.spec = spec;
  }

  public Optional<String> run() {
    if (!(testCase instanceof PreField)) {
      throw new RuntimeException("TestCase runner accepts only test cases with Pre field");
    }
    BeaconState latestState = ((PreField) testCase).getPre(spec.getConstants());
    Optional<String> processingError;

    BeaconState stateBackup = latestState.createMutableCopy();
    if (testCase instanceof SanitySlotsCase) {
      Pair<Optional<String>, BeaconState> processingSlots =
          processSlots(((SanitySlotsCase) testCase).getSlots(), latestState);
      processingError = processingSlots.getValue0();
      if (!processingError.isPresent()) {
        latestState = processingSlots.getValue1();
      }
    } else if (testCase instanceof SanityBlocksCase) {
      Pair<Optional<String>, BeaconState> processingBlocks =
          processBlocks(((SanityBlocksCase) testCase).getBlocks(spec.getConstants()), latestState);
      processingError = processingBlocks.getValue0();
      if (!processingError.isPresent()) {
        latestState = processingBlocks.getValue1();
      }
    } else if (testCase instanceof OperationAttestationCase) {
      processingError =
          processAttestation(
              ((OperationAttestationCase) testCase).getAttestation(spec.getConstants()),
              latestState);
    } else if (testCase instanceof OperationDepositCase) {
      processingError = processDeposit(((OperationDepositCase) testCase).getDeposit(), latestState);
    } else if (testCase instanceof OperationAttesterSlashingCase) {
      processingError =
          processAttesterSlashing(
              ((OperationAttesterSlashingCase) testCase).getAttesterSlashing(spec.getConstants()),
              latestState);
    } else if (testCase instanceof OperationProposerSlashingCase) {
      processingError =
          processProposerSlashing(
              ((OperationProposerSlashingCase) testCase).getProposerSlashing(), latestState);
    } else if (testCase instanceof OperationTransferCase) {
      processingError =
          processTransfer(((OperationTransferCase) testCase).getTransfer(), latestState);
    } else if (testCase instanceof OperationVoluntaryExitCase) {
      processingError =
          processVoluntaryExit(
              ((OperationVoluntaryExitCase) testCase).getVoluntaryExit(), latestState);
    } else if (testCase instanceof OperationBlockHeaderCase) {
      processingError =
          processBlockHeader(
              ((OperationBlockHeaderCase) testCase).getBlock(spec.getConstants()), latestState);
    } else if (testCase instanceof CrosslinksProcessingCase) {
      processingError = processCrosslinks(latestState);
    } else if (testCase instanceof FinalUpdatesProcessingCase) {
      processingError = processFinalUpdates(latestState);
    } else if (testCase instanceof FinalizationProcessingCase) {
      processingError = processJustificationAndFinalization(latestState);
    } else if (testCase instanceof RegistryUpdatesProcessingCase) {
      processingError = processRegistryUpdates(latestState);
    } else if (testCase instanceof SlashingsProcessingCase) {
      processingError = processSlashings(latestState);
    } else {
      throw new RuntimeException("This type of state test is not supported");
    }

    if (processingError.isPresent()) {
      latestState = stateBackup;
    }
    if (!(testCase instanceof PostField)) {
      throw new RuntimeException("TestCase runner accepts only test cases with Post field");
    }
    if (((PostField) testCase).getPost(spec.getConstants()) == null) { // XXX: Not changed
      return StateComparator.compare(((PreField) testCase).getPre(spec.getConstants()), latestState, spec);
    } else {
      Optional compareResult =
          StateComparator.compare(((PostField) testCase).getPost(spec.getConstants()), latestState, spec);
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
      //spec.process_crosslinks((MutableBeaconState) state);
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
    PerEpochTransition perEpochTransition = new PerEpochTransition(spec);
    StateTransition<BeaconStateEx> perSlotTransition = new PerSlotTransition(spec);
    EmptySlotTransition slotTransition =
        new EmptySlotTransition(
            new ExtendedSlotTransition(perEpochTransition, perSlotTransition, spec));

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
}
