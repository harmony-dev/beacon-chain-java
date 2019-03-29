package org.ethereum.beacon.consensus.verifier;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

/**
 * An interface for verifiers that check {@link BeaconState} correctness.
 *
 * @see BeaconStateRootMatcher
 * @see VerificationResult
 */
public interface BeaconStateVerifier {

  static BeaconStateVerifier createDefault(BeaconChainSpec spec) {
    return new BeaconStateRootMatcher(spec);
  }

  /**
   * Runs state verifications.
   *
   * @param state a state to verify.
   * @param block a block that verifying state does relate to.
   * @return result of verification.
   */
  VerificationResult verify(BeaconState state, BeaconBlock block);
}
