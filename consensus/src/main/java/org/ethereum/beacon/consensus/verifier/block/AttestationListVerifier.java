package org.ethereum.beacon.consensus.verifier.block;

import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.spec.SpecConstants;

/**
 * Verifies attestation list.
 *
 * @see Attestation
 */
public class AttestationListVerifier extends OperationListVerifier<Attestation> {

  public AttestationListVerifier(
      OperationVerifier<Attestation> operationVerifier, SpecConstants specConstants) {
    super(
        operationVerifier,
        block -> block.getMessage().getBody().getAttestations(),
        specConstants.getMaxAttestations());
  }

  @Override
  protected Class<Attestation> getType() {
    return Attestation.class;
  }
}
