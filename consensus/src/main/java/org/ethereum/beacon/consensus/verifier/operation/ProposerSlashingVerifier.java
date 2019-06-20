package org.ethereum.beacon.consensus.verifier.operation;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.spec.SpecCommons;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.ProposerSlashing;

/**
 * Verifies {@link ProposerSlashing} beacon chain operation.
 *
 * @see ProposerSlashing
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.0/specs/core/0_beacon-chain.md#proposer-slashings">Proposer
 *     slashings</a> in the spec.
 */
public class ProposerSlashingVerifier implements OperationVerifier<ProposerSlashing> {

  private BeaconChainSpec spec;

  public ProposerSlashingVerifier(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(ProposerSlashing proposerSlashing, BeaconState state) {
    try {
      spec.verify_proposer_slashing(state, proposerSlashing);
      return VerificationResult.PASSED;
    } catch (SpecCommons.SpecAssertionFailed e) {
      return VerificationResult.failedResult(e.getMessage());
    }
  }
}
