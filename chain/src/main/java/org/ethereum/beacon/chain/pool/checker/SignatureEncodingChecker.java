package org.ethereum.beacon.chain.pool.checker;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.crypto.BLS381;

public class SignatureEncodingChecker implements AttestationChecker {

  @Override
  public boolean check(ReceivedAttestation attestation) {
    return BLS381.Signature.validate(attestation.getMessage().getSignature());
  }
}
