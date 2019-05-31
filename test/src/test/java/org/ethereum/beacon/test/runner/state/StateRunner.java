package org.ethereum.beacon.test.runner.state;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.spec.SpecCommons;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.consensus.verifier.operation.AttestationVerifier;
import org.ethereum.beacon.consensus.verifier.operation.AttesterSlashingVerifier;
import org.ethereum.beacon.consensus.verifier.operation.DepositVerifier;
import org.ethereum.beacon.consensus.verifier.operation.ProposerSlashingVerifier;
import org.ethereum.beacon.consensus.verifier.operation.TransferVerifier;
import org.ethereum.beacon.consensus.verifier.operation.VoluntaryExitVerifier;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockHeader;
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

  public StateRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof StateTestCase)) {
      throw new RuntimeException("TestCase runner accepts only StateTestCase.class as input!");
    }
    this.testCase = (StateTestCase) testCase;
    this.spec = spec;
  }

  public Optional<String> run() {
    BeaconState initialState = buildInitialState(spec, testCase.getPre());
    Optional<String> err = StateComparator.compare(testCase.getPre(), initialState);
    if (err.isPresent()) {
      return Optional.of("Initial state parsed incorrectly: " + err.get());
    }
    BeaconState latestState = initialState;
    Optional<String> processingError;

    BeaconState stateBackup = latestState.createMutableCopy();
    if (testCase.getDeposit() != null) {
      processingError = processDeposit(testCase.getDepositOperation(), latestState);
    } else if (testCase.getAttestation() != null) {
      processingError = processAttestation(testCase.getAttestationOperation(), latestState);
    } else if (testCase.getAttesterSlashing() != null) {
      processingError = processAttesterSlashing(testCase.getAttesterSlashingOperation(), latestState);
    } else if (testCase.getProposerSlashing() != null) {
      processingError = processProposerSlashing(testCase.getProposerSlashingOperation(), latestState);
    } else if (testCase.getTransfer() != null) {
      processingError = processTransfer(testCase.getTransferOperation(), latestState);
    } else if (testCase.getVoluntaryExit() != null) {
      processingError = processVoluntaryExit(testCase.getVoluntaryExitOperation(), latestState);
    } else if (testCase.getBlock() != null) {
      processingError = processBlockHeader(testCase.getBeaconBlock(), latestState);
    } else {
      throw new RuntimeException("This type of state test is not supported");
    }
    if (processingError.isPresent()) {
      latestState = stateBackup;
    }

    if (testCase.getPost() == null) { // XXX: Not changed
      return StateComparator.compare(testCase.getPre(), latestState);
    } else {
      Optional compareResult = StateComparator.compare(testCase.getPost(), latestState);
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

  private Optional<String> processAttesterSlashing(AttesterSlashing attesterSlashing, BeaconState state) {
    AttesterSlashingVerifier slashingVerifier = new AttesterSlashingVerifier(spec);
    return processOperation(
        attesterSlashing,
        state,
        slashingVerifier,
        objects ->
            spec.process_attester_slashing(
                (MutableBeaconState) objects.getValue1(), objects.getValue0()));
  }

  private Optional<String> processProposerSlashing(ProposerSlashing proposerSlashing, BeaconState state) {
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
            spec.process_transfer(
                (MutableBeaconState) objects.getValue1(), objects.getValue0()));
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
    } catch (SpecCommons.SpecAssertionFailed | IllegalArgumentException ex) {
      return Optional.of(ex.getMessage());
    }
  }

  private BeaconState buildInitialState(BeaconChainSpec spec, BeaconStateData stateData) {
    return StateTestUtils.parseBeaconState(spec.getConstants(), stateData);
  }
}
