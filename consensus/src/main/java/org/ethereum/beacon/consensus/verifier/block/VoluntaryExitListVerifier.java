package org.ethereum.beacon.consensus.verifier.block;

import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.envelops.SignedVoluntaryExit;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.spec.SpecConstants;

/**
 * Verifies exit operation list.
 *
 * @see VoluntaryExit
 */
public class VoluntaryExitListVerifier extends OperationListVerifier<SignedVoluntaryExit> {

  public VoluntaryExitListVerifier(OperationVerifier<SignedVoluntaryExit> operationVerifier, SpecConstants specConstants) {
    super(operationVerifier, block -> block.getMessage().getBody().getVoluntaryExits(), specConstants.getMaxVoluntaryExits());
  }

  @Override
  protected Class<SignedVoluntaryExit> getType() {
    return SignedVoluntaryExit.class;
  }
}
