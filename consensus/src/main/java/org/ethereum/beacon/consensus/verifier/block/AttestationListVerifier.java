package org.ethereum.beacon.consensus.verifier.block;

import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.spec.ChainSpec;

/**
 * Verifies attestation list.
 *
 * @see Attestation
 */
public class AttestationListVerifier extends OperationListVerifier<Attestation> {

  public AttestationListVerifier(
      OperationVerifier<Attestation> operationVerifier, ChainSpec chainSpec) {
    super(
        operationVerifier,
        block -> block.getBody().getAttestations(),
        chainSpec.getMaxAttestations());
  }

  @Override
  protected Class<Attestation> getType() {
    return Attestation.class;
  }
}
