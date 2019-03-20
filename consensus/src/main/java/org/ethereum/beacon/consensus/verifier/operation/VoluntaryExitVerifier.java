package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;
import static org.ethereum.beacon.core.spec.SignatureDomains.VOLUNTARY_EXIT;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.state.ValidatorRecord;

/**
 * Verifies {@link VoluntaryExit} beacon chain operation.
 *
 * @see VoluntaryExit
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/0.4.0/specs/core/0_beacon-chain.md#voluntary-exits-1">Voluntary
 *     exits</a> in the spec.
 */
public class VoluntaryExitVerifier implements OperationVerifier<VoluntaryExit> {

  private SpecHelpers spec;

  public VoluntaryExitVerifier(SpecHelpers spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(VoluntaryExit voluntaryExit, BeaconState state) {
    spec.checkIndexRange(state, voluntaryExit.getValidatorIndex());

    ValidatorRecord validator = state.getValidatorRegistry().get(voluntaryExit.getValidatorIndex());

    // Verify that validator.exit_epoch >
    // get_delayed_activation_exit_epoch(get_current_epoch(state))
    if (!validator
        .getExitEpoch()
        .greater(spec.get_delayed_activation_exit_epoch(spec.get_current_epoch(state)))) {
      return failedResult(
          "ACTIVATION_EXIT_DELAY exceeded, min exit epoch %s, got %s",
          state.getSlot().plus(spec.getConstants().getActivationExitDelay()),
          validator.getExitEpoch());
    }

    // Verify that get_current_epoch(state) >= exit.epoch
    if (!spec.get_current_epoch(state).greaterEqual(voluntaryExit.getEpoch())) {
      return failedResult("exit.epoch must be greater or equal to current_epoch");
    }

    // Let exit_message = hash_tree_root(
    //    VoluntaryExitVerifier(
    //        epoch=exit.epoch,
    //        validator_index=exit.validator_index,
    //        signature=EMPTY_SIGNATURE)).
    // Verify that bls_verify(
    //    pubkey=validator.pubkey,
    //    message=exit_message,
    //    signature=exit.signature,
    //    domain=get_domain(state.fork, exit.epoch, DOMAIN_EXIT)).
    if (!spec.bls_verify(
        validator.getPubKey(),
        spec.signed_root(voluntaryExit, "signature"),
        voluntaryExit.getSignature(),
        spec.get_domain(state.getFork(), voluntaryExit.getEpoch(), VOLUNTARY_EXIT))) {
      return failedResult("failed to verify signature");
    }

    return PASSED;
  }
}
