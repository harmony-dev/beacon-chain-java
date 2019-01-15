package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;

import org.ethereum.beacon.core.operations.CustodyReseed;

/**
 * Verifies custody reseed list.
 *
 * <p><strong>Note:</strong> to be enabled in Phase 1.
 *
 * @see CustodyReseed
 */
public class CustodyReseedListVerifier extends OperationListVerifier<CustodyReseed> {

  protected CustodyReseedListVerifier() {
    super((operation, state) -> PASSED, block -> block.getBody().getCustodyReseeds(), 0);
  }

  @Override
  protected Class<CustodyReseed> getType() {
    return CustodyReseed.class;
  }
}
