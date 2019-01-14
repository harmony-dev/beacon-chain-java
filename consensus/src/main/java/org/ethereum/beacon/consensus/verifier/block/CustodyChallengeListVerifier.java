package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;

import org.ethereum.beacon.core.operations.CustodyChallenge;

/**
 * Verifies custody challenge list.
 *
 * <p><strong>Note:</strong> to be enabled in Phase 1.
 *
 * @see CustodyChallenge
 */
public class CustodyChallengeListVerifier extends OperationListVerifier<CustodyChallenge> {

  public CustodyChallengeListVerifier() {
    super((operation, state) -> PASSED, block -> block.getBody().getCustodyChallenges(), 0);
  }

  @Override
  protected Class<CustodyChallenge> getType() {
    return CustodyChallenge.class;
  }
}
