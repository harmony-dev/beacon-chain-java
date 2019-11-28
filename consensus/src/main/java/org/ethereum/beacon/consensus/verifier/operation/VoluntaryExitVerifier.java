package org.ethereum.beacon.consensus.verifier.operation;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.spec.SpecCommons;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.VoluntaryExit;

/**
 * Verifies {@link VoluntaryExit} beacon chain operation.
 *
 * @see VoluntaryExit
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.9.2/specs/core/0_beacon-chain.md#voluntary-exits">Voluntary
 *     exits</a> in the spec.
 */
public class VoluntaryExitVerifier implements OperationVerifier<VoluntaryExit> {

  private BeaconChainSpec spec;

  public VoluntaryExitVerifier(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(VoluntaryExit voluntaryExit, BeaconState state) {
    try {
      spec.verify_voluntary_exit(state, voluntaryExit);
      return VerificationResult.PASSED;
    } catch (SpecCommons.SpecAssertionFailed e) {
      return VerificationResult.failedResult(e.getMessage());
    }
  }
}
