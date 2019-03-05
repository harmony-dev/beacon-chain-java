package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.operations.Transfer;

/**
 * Verifies transfers list.
 *
 * @see Transfer
 */
public class TransferListVerifier extends OperationListVerifier<Transfer> {

  protected TransferListVerifier(OperationVerifier<Transfer> operationVerifier, SpecHelpers spec) {
    super(
        operationVerifier,
        block -> block.getBody().getTransfers(),
        spec.getConstants().getMaxTransfers());

    addCustomVerifier(
        ((transfers, state) -> {
          for (Transfer t1 : transfers) {
            for (Transfer t2 : transfers) {
              if (t1.equals(t2)) {
                return failedResult("two equal transfers have been found");
              }
            }
          }

          return PASSED;
        }));
  }

  @Override
  protected Class<Transfer> getType() {
    return Transfer.class;
  }
}
