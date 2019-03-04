package org.ethereum.beacon.consensus.verifier.block;

import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.spec.SpecConstants;

/**
 * Verifies exit operation list.
 *
 * @see VoluntaryExit
 */
public class VoluntaryExitListVerifier extends OperationListVerifier<VoluntaryExit> {

  public VoluntaryExitListVerifier(OperationVerifier<VoluntaryExit> operationVerifier, SpecConstants specConstants) {
    super(operationVerifier, block -> block.getBody().getExits(), specConstants.getMaxVoluntaryExits());
  }

  @Override
  protected Class<VoluntaryExit> getType() {
    return VoluntaryExit.class;
  }
}
