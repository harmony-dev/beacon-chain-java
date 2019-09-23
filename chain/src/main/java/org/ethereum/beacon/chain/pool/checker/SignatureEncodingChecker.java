package org.ethereum.beacon.chain.pool.checker;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.registry.ProcessedAttestations;
import org.ethereum.beacon.crypto.BLS381;

/**
 * Checks signature encoding format.
 *
 * <p>Attestations with invalid signature encoding SHOULD be considered as invalid.
 *
 * <p>This is relatively heavy check in terms of CPU cycles as it involves a few operations on a
 * field numbers and one point multiplication. It's recommended to put this checker after {@link
 * ProcessedAttestations} registry.
 *
 * <p><strong>Note:</strong> this implementation is not thread-safe.
 */
public class SignatureEncodingChecker implements AttestationChecker {

  @Override
  public boolean check(ReceivedAttestation attestation) {
    return BLS381.Signature.validate(attestation.getMessage().getSignature());
  }
}
