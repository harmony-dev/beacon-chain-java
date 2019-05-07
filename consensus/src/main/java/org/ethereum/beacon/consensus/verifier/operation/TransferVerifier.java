package org.ethereum.beacon.consensus.verifier.operation;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.spec.SpecCommons;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Transfer;

/**
 * Verifies {@link Transfer} beacon chain operation.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.0/specs/core/0_beacon-chain.md#transfers">Transfers</a>
 *     section in the spec.
 */
public class TransferVerifier implements OperationVerifier<Transfer> {

  private final BeaconChainSpec spec;

  public TransferVerifier(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(Transfer transfer, BeaconState state) {
    try {
      spec.verify_transfer(state, transfer);
      return VerificationResult.PASSED;
    } catch (SpecCommons.SpecAssertionFailed e) {
      String error = e.getStackTrace().length > 0 ? e.getStackTrace()[1].toString() : "SpecAssertion";
      return VerificationResult.failedResult(error);
    }
  }
}
