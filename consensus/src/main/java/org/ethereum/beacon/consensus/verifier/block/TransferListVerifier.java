package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.operations.Transfer;

/**
 * Verifies transfers list.
 *
 * @see Transfer
 */
public class TransferListVerifier extends OperationListVerifier<Transfer> {

  public TransferListVerifier(OperationVerifier<Transfer> operationVerifier, SpecHelpers spec) {
    super(
        operationVerifier,
        block -> block.getBody().getTransfers(),
        spec.getConstants().getMaxTransfers());

    addCustomVerifier(
        ((transfers, state) -> {
          List<Transfer> allTransfers =
              StreamSupport.stream(transfers.spliterator(), false).collect(Collectors.toList());
          Set<Transfer> distinctTransfers =
              StreamSupport.stream(transfers.spliterator(), false).collect(Collectors.toSet());

          if (allTransfers.size() > distinctTransfers.size()) {
            return failedResult("two equal transfers have been found");
          }

          return PASSED;
        }));
  }

  @Override
  protected Class<Transfer> getType() {
    return Transfer.class;
  }
}
