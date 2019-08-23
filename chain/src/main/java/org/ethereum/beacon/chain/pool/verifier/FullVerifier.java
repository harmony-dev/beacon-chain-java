package org.ethereum.beacon.chain.pool.verifier;

import java.util.Collections;
import java.util.List;
import org.ethereum.beacon.chain.pool.AttestationPool;
import org.ethereum.beacon.chain.pool.AttestationVerifier;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class FullVerifier implements AttestationVerifier {

  private final AttestationStateVerifier stateVerifier;
  private final AttestationSignatureVerifier signatureVerifier;

  public FullVerifier(
      AttestationStateVerifier stateVerifier, AttestationSignatureVerifier signatureVerifier) {
    this.stateVerifier = stateVerifier;
    this.signatureVerifier = signatureVerifier;

    Flux.from(stateVerifier.valid())
        .bufferTimeout(AttestationPool.VERIFIER_BUFFER_SIZE, AttestationPool.VERIFIER_INTERVAL)
        .subscribe(signatureVerifier::in);
  }

  @Override
  public Publisher<ReceivedAttestation> valid() {
    return signatureVerifier.valid();
  }

  @Override
  public void in(ReceivedAttestation attestation) {
    stateVerifier.in(Collections.singletonList(attestation));
  }

  @Override
  public void batchIn(List<ReceivedAttestation> batch) {
    stateVerifier.in(batch);
  }

  @Override
  public Publisher<ReceivedAttestation> invalid() {
    return Flux.concat(stateVerifier.invalid(), signatureVerifier.invalid());
  }
}
