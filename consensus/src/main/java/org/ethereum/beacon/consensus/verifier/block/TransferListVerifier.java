package org.ethereum.beacon.consensus.verifier.block;

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
  }

  @Override
  protected Class<Transfer> getType() {
    return Transfer.class;
  }
}
