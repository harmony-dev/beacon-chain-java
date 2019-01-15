package org.ethereum.beacon.consensus.verifier.block;

import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.spec.ChainSpec;

/**
 * Verifies exit operation list.
 *
 * @see Exit
 */
public class ExitListVerifier extends OperationListVerifier<Exit> {

  public ExitListVerifier(OperationVerifier<Exit> operationVerifier, ChainSpec chainSpec) {
    super(operationVerifier, block -> block.getBody().getExits(), chainSpec.getMaxExits());
  }

  @Override
  protected Class<Exit> getType() {
    return Exit.class;
  }
}
