package org.ethereum.beacon.consensus.verifier.block;

import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.spec.SpecConstants;

/**
 * Verifies proposer slashing list.
 *
 * @see ProposerSlashing
 */
public class ProposerSlashingListVerifier extends OperationListVerifier<ProposerSlashing> {

  public ProposerSlashingListVerifier(
      OperationVerifier<ProposerSlashing> operationVerifier, SpecConstants specConstants) {
    super(
        operationVerifier,
        block -> block.getBody().getProposerSlashings(),
        specConstants.getMaxProposerSlashings());
  }

  @Override
  protected Class<ProposerSlashing> getType() {
    return ProposerSlashing.class;
  }
}
