package org.ethereum.beacon.consensus.verifier.block;

import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.spec.ChainSpec;

/**
 * Verifies casper slashing list.
 *
 * @see CasperSlashing
 */
public class CasperSlashingListVerifier extends OperationListVerifier<CasperSlashing> {

  public CasperSlashingListVerifier(
      OperationVerifier<CasperSlashing> operationVerifier, ChainSpec chainSpec) {
    super(
        operationVerifier,
        block -> block.getBody().getCasperSlashings(),
        chainSpec.getMaxCasperSlashings());
  }

  @Override
  protected Class<CasperSlashing> getType() {
    return CasperSlashing.class;
  }
}
