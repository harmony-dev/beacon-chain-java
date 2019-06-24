package org.ethereum.beacon.consensus.verifier.operation;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.spec.SpecCommons;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;

/**
 * Verifies {@link AttesterSlashing} beacon chain operation.
 *
 * @see AttesterSlashing
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/core/0_beacon-chain.md#attester-slashings">
 *     Attester slashings</a> in the spec.
 */
public class AttesterSlashingVerifier implements OperationVerifier<AttesterSlashing> {

  private BeaconChainSpec spec;

  public AttesterSlashingVerifier(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(AttesterSlashing attesterSlashing, BeaconState state) {
    try {
      spec.verify_attester_slashing(state, attesterSlashing);
      return VerificationResult.PASSED;
    } catch (SpecCommons.SpecAssertionFailed e) {
      return VerificationResult.failedResult(e.getMessage());
    }
  }
}
