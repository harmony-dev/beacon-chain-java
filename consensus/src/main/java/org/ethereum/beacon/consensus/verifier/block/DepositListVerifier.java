package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;

import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Verifies deposit list.
 *
 * @see Deposit
 */
public class DepositListVerifier extends OperationListVerifier<Deposit> {

  public DepositListVerifier(OperationVerifier<Deposit> operationVerifier, ChainSpec chainSpec) {
    super(operationVerifier, block -> block.getBody().getDeposits(), chainSpec.getMaxDeposits());

    addCustomVerifier(
        deposits -> {
          if (deposits.size() > 0) {
            UInt64 expectedIndex = deposits.get(0).getDepositIndex();
            for (Deposit deposit : deposits) {
              if (!deposit.getDepositIndex().equals(expectedIndex)) {
                return failedResult(
                    "inclusion order is broken, expected index %d but got %d",
                    expectedIndex, deposit.getDepositIndex());
              }
              expectedIndex = expectedIndex.increment();
            }
          }

          return PASSED;
        });
  }

  @Override
  protected Class<Deposit> getType() {
    return Deposit.class;
  }
}
