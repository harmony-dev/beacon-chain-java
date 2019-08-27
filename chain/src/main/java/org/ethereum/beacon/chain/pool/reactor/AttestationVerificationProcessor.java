package org.ethereum.beacon.chain.pool.reactor;

import java.util.List;
import java.util.stream.Stream;
import org.ethereum.beacon.chain.pool.CheckedAttestation;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.verifier.BatchVerifier;
import org.ethereum.beacon.chain.pool.verifier.VerificationResult;
import org.ethereum.beacon.stream.AbstractDelegateProcessor;

public class AttestationVerificationProcessor
    extends AbstractDelegateProcessor<List<ReceivedAttestation>, CheckedAttestation> {

  private final BatchVerifier verifier;

  public AttestationVerificationProcessor(BatchVerifier verifier) {
    this.verifier = verifier;
  }

  @Override
  protected void hookOnNext(List<ReceivedAttestation> batch) {
    VerificationResult result = verifier.verify(batch);
    Stream.concat(
            result.getValid().stream().map(att -> new CheckedAttestation(true, att)),
            result.getInvalid().stream().map(att -> new CheckedAttestation(false, att)))
        .forEach(this::publishOut);
  }
}
