package org.ethereum.beacon.consensus.verifier;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

/**
 * An interface for verifiers that check {@link BeaconState} correctness.
 *
 * @see BeaconStateRootMatcher
 * @see VerificationResult
 */
public interface BeaconStateVerifier {

  /**
   * Runs state verifications.
   *
   * @param block a block that verifying state does relate to.
   * @param state a state to verify.
   * @return result of verification.
   */
  VerificationResult verify(BeaconBlock block, BeaconState state);
}
