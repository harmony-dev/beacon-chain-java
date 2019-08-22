package org.ethereum.beacon.chain.pool.verifier;

import org.ethereum.beacon.chain.pool.AttestationVerifier;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class AttestationPoolVerifierImpl implements AttestationVerifier {

  private final AttestationStateVerifier stateVerifier;
  private final AttestationSignatureVerifier signatureVerifier;

  public AttestationPoolVerifierImpl(
      AttestationStateVerifier stateVerifier, AttestationSignatureVerifier signatureVerifier) {
    this.stateVerifier = stateVerifier;
    this.signatureVerifier = signatureVerifier;
  }

  @Override
  public Publisher<ReceivedAttestation> valid() {
    return signatureVerifier.valid();
  }

  @Override
  public void in(ReceivedAttestation attestation) {
    stateVerifier.inbound(attestation);
  }

  @Override
  public Publisher<ReceivedAttestation> invalid() {
    return Flux.concat(stateVerifier.invalid(), signatureVerifier.invalid());
  }
}
