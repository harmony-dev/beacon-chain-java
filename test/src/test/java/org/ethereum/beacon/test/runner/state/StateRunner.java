package org.ethereum.beacon.test.runner.state;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.verifier.operation.AttestationVerifier;
import org.ethereum.beacon.consensus.verifier.operation.DepositVerifier;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.test.StateTestUtils;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.state.StateTestCase;
import org.ethereum.beacon.test.type.state.StateTestCase.BeaconStateData;

import java.util.Optional;

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
    if (testCase.getDeposit() != null) {
      processDeposit(testCase.getDepositOperation(), latestState);
    } else if (testCase.getAttestation() != null) {
      processAttestation(testCase.getAttestationOperation(), latestState);
    } else {
      throw new RuntimeException("Only Attestation and Deposit test cases are implemented!!!");
    }

    if (testCase.getPost() == null) { // XXX: Not changed
      return StateComparator.compare(testCase.getPre(), latestState);
    } else {
      return StateComparator.compare(testCase.getPost(), latestState);
    }
  }

  private void processDeposit(Deposit deposit, BeaconState state) {
    try {
      DepositVerifier depositVerifier = new DepositVerifier(spec);
      assert depositVerifier.verify(deposit, state).isPassed();
      spec.process_deposit((MutableBeaconState) state, deposit);
    } catch (Exception | AssertionError ex) {
      // XXX: there could be invalid deposit, it's ok
    }
  }

  private void processAttestation(Attestation attestation, BeaconState state) {
    try {
      AttestationVerifier attestationVerifier = new AttestationVerifier(spec);
      assert attestationVerifier.verify(attestation, state).isPassed();
      spec.process_attestation((MutableBeaconState) state, attestation);
    } catch (Exception | AssertionError ex) {
      // XXX: there could be invalid attestation, it's ok
    }
  }

  private BeaconState buildInitialState(BeaconChainSpec spec, BeaconStateData stateData) {
    return StateTestUtils.parseBeaconState(spec.getConstants(), stateData);
  }
}
