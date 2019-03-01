package org.ethereum.beacon.consensus.verifier.block;

import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.spec.ChainSpec;

/**
 * Verifies exit operation list.
 *
 * @see VoluntaryExit
 */
public class ExitListVerifier extends OperationListVerifier<VoluntaryExit> {

  public ExitListVerifier(OperationVerifier<VoluntaryExit> operationVerifier, ChainSpec chainSpec) {
    super(operationVerifier, block -> block.getBody().getExits(), chainSpec.getMaxVoluntaryExits());
  }

  @Override
  protected Class<VoluntaryExit> getType() {
    return VoluntaryExit.class;
  }
}
