package org.ethereum.beacon.chain.pool.reactor;

import java.util.List;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.verifier.BatchVerifier;
import org.ethereum.beacon.chain.pool.verifier.VerificationResult;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
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

  private final SimpleProcessor<ReceivedAttestation> valid;
  private final SimpleProcessor<IndexedAttestation> validIndexed;
  private final SimpleProcessor<ReceivedAttestation> invalid;

  public VerificationProcessor(
      BatchVerifier verifier, Scheduler scheduler, Publisher<List<ReceivedAttestation>> source) {
    this.verifier = verifier;

    this.valid = new SimpleProcessor<>(scheduler, "VerificationProcessor.valid");
    this.validIndexed = new SimpleProcessor<>(scheduler, "VerificationProcessor.validIndexed");
    this.invalid = new SimpleProcessor<>(scheduler, "VerificationProcessor.invalid");

    Flux.from(source).publishOn(scheduler.toReactor()).subscribe(this::hookOnNext);
  }

  private void hookOnNext(List<ReceivedAttestation> batch) {
    VerificationResult result = verifier.verify(batch);
    result.getValid().forEach(valid::onNext);
    result.getValidIndexed().forEach(validIndexed::onNext);
    result.getInvalid().forEach(invalid::onNext);
  }

  public Publisher<ReceivedAttestation> getValid() {
    return valid;
  }

  public SimpleProcessor<IndexedAttestation> getValidIndexed() {
    return validIndexed;
  }

  public Publisher<ReceivedAttestation> getInvalid() {
    return invalid;
  }
}
