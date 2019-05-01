package org.ethereum.beacon.consensus.verifier.block;

import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.spec.SpecConstants;

/**
 * Verifies attester slashing list.
 *
 * @see AttesterSlashing
 */
public class AttesterSlashingListVerifier extends OperationListVerifier<AttesterSlashing> {

  public AttesterSlashingListVerifier(
      OperationVerifier<AttesterSlashing> operationVerifier, SpecConstants specConstants) {
    super(
        operationVerifier,
        block -> block.getBody().getAttesterSlashings(),
        specConstants.getMaxAttesterSlashings());
  }

  @Override
  protected Class<AttesterSlashing> getType() {
    return AttesterSlashing.class;
  }
}
