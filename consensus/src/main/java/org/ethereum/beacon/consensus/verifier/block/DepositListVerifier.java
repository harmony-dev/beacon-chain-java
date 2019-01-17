package org.ethereum.beacon.consensus.verifier.block;

import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;

/**
 * Verifies deposit list.
 *
 * @see Deposit
 */
public class DepositListVerifier extends OperationListVerifier<Deposit> {

  public DepositListVerifier(OperationVerifier<Deposit> operationVerifier, ChainSpec chainSpec) {
    super(operationVerifier, block -> block.getBody().getDeposits(), chainSpec.getMaxDeposits());
  }

  @Override
  protected Class<Deposit> getType() {
    return Deposit.class;
  }
}
