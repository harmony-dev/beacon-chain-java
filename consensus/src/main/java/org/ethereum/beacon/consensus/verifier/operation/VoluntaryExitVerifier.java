package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;
import static org.ethereum.beacon.core.spec.SignatureDomains.VOLUNTARY_EXIT;

import org.ethereum.beacon.consensus.BeaconChainSpec;
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

  private BeaconChainSpec spec;

  public VoluntaryExitVerifier(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(VoluntaryExit voluntaryExit, BeaconState state) {
    spec.checkIndexRange(state, voluntaryExit.getValidatorIndex());

    ValidatorRecord validator = state.getValidatorRegistry().get(voluntaryExit.getValidatorIndex());

    // Verify the validator has not yet exited
    if (!validator.getExitEpoch().equals(spec.getConstants().getFarFutureEpoch())) {
      return failedResult("validator #%s has already exited", voluntaryExit.getValidatorIndex());
    }

    // Verify the validator has not initiated an exit
    if (validator.getInitiatedExit()) {
      return failedResult("validator #%s has already initiated an exit",
          voluntaryExit.getValidatorIndex());
    }

    // Exits must specify an epoch when they become valid; they are not valid before then
    if (!(spec.get_current_epoch(state).greaterEqual(voluntaryExit.getEpoch()) &&
        spec.get_current_epoch(state).minus(validator.getActivationEpoch())
            .greaterEqual(spec.getConstants().getPersistentCommitteePeriod()))) {
      return failedResult("validator #%s exit epoch boundaries are violated, min exit epoch %s but got %s",
          validator.getActivationEpoch().plus(spec.getConstants().getPersistentCommitteePeriod()),
          spec.get_current_epoch(state));
    }

    /* assert bls_verify(
         pubkey=validator.pubkey,
         message_hash=signed_root(exit),
         signature=exit.signature,
         domain=get_domain(state.fork, exit.epoch, DOMAIN_VOLUNTARY_EXIT)
       ) */
    if (!spec.bls_verify(
        validator.getPubKey(),
        spec.signed_root(voluntaryExit),
        voluntaryExit.getSignature(),
        spec.get_domain(state.getFork(), voluntaryExit.getEpoch(), VOLUNTARY_EXIT))) {
      return failedResult("failed to verify signature");
    }

    return PASSED;
  }
}
