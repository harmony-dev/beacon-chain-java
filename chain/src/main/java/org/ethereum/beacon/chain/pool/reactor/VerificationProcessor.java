package org.ethereum.beacon.chain.pool.reactor;

import java.util.List;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.verifier.BatchVerifier;
import org.ethereum.beacon.chain.pool.verifier.VerificationResult;
import org.ethereum.beacon.stream.OutsourcePublisher;
import reactor.core.publisher.Flux;

/**
 * Passes attestations through {@link BatchVerifier}.
 *
 * <p>Input:
 *
 * <ul>
 *   <li>batches of {@link ReceivedAttestation}
 * </ul>
 *
 * <p>Output:
 *
 * <ul>
 *   <li>attestations successfully passed verification
 *   <li>invalid attestations
 * </ul>
 */
public class VerificationProcessor {

  private final BatchVerifier verifier;

  private final OutsourcePublisher<ReceivedAttestation> valid = new OutsourcePublisher<>();
  private final OutsourcePublisher<ReceivedAttestation> invalid = new OutsourcePublisher<>();

  public VerificationProcessor(BatchVerifier verifier, Flux<List<ReceivedAttestation>> source) {
    this.verifier = verifier;

    source.subscribe(this::hookOnNext);
  }

  private void hookOnNext(List<ReceivedAttestation> batch) {
    VerificationResult result = verifier.verify(batch);
    result.getValid().forEach(valid::publishOut);
    result.getInvalid().forEach(invalid::publishOut);
  }

  public Flux<ReceivedAttestation> getValid() {
    return valid;
  }

  public Flux<ReceivedAttestation> getInvalid() {
    return invalid;
  }
}
