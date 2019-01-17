package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;

import org.ethereum.beacon.core.operations.CustodyResponse;

/**
 * Verifies custody response list.
 *
 * <p><strong>Note:</strong> to be enabled in Phase 1.
 *
 * @see CustodyResponse
 */
public class CustodyResponseListVerifier extends OperationListVerifier<CustodyResponse> {

  protected CustodyResponseListVerifier() {
    super((operation, state) -> PASSED, block -> block.getBody().getCustodyResponses(), 0);
  }

  @Override
  protected Class<CustodyResponse> getType() {
    return null;
  }
}
