package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.core.spec.SignatureDomains.RANDAO;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.state.ValidatorRecord;

/**
 * Verifies RANDAO reveal.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.1/specs/core/0_beacon-chain.md#randao">RANDAO</a>
 *     in the spec.
 */
public class RandaoVerifier implements BeaconBlockVerifier {

  private BeaconChainSpec spec;

  public RandaoVerifier(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(BeaconBlock block, BeaconState state) {
    try {
      spec.verify_randao(state, block);
      return VerificationResult.PASSED;
    } catch (Exception e) {
      return VerificationResult.failedResult(
          "RANDAO reveal verification failed: %s", e.getMessage());
    }
  }
}
