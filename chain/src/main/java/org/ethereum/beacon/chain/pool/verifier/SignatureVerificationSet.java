package org.ethereum.beacon.chain.pool.verifier;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import tech.pegasys.artemis.util.uint.UInt64;

final class SignatureVerificationSet {
  private final PublicKey bit0AggregateKey;
  private final PublicKey bit1AggregateKey;
  private final UInt64 domain;
  private final ReceivedAttestation attestation;

  SignatureVerificationSet(
      PublicKey bit0AggregateKey,
      PublicKey bit1AggregatedKey,
      UInt64 domain,
      ReceivedAttestation attestation) {
    this.bit0AggregateKey = bit0AggregateKey;
    this.bit1AggregateKey = bit1AggregatedKey;
    this.domain = domain;
    this.attestation = attestation;
  }

  public PublicKey getBit0AggregateKey() {
    return bit0AggregateKey;
  }

  public PublicKey getBit1AggregateKey() {
    return bit1AggregateKey;
  }

  public UInt64 getDomain() {
    return domain;
  }

  public ReceivedAttestation getAttestation() {
    return attestation;
  }
}
