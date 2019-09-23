package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.SignatureEncodingChecker;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Passes attestations through {@link SignatureEncodingChecker}.
 *
 * <p>Input:
 *
 * <ul>
 *   <li>attestations
 * </ul>
 *
 * <p>Output:
 *
 * <ul>
 *   <li>attestations with valid signature encoding
 *   <li>invalid attestations
 * </ul>
 */
public class SignatureEncodingProcessor {

  private final SignatureEncodingChecker checker;
  private final SimpleProcessor<ReceivedAttestation> valid;
  private final SimpleProcessor<ReceivedAttestation> invalid;

  public SignatureEncodingProcessor(
      SignatureEncodingChecker checker,
      Scheduler scheduler,
      Publisher<ReceivedAttestation> source) {
    this.checker = checker;

    this.valid = new SimpleProcessor<>(scheduler, "SignatureEncodingProcessor.valid");
    this.invalid = new SimpleProcessor<>(scheduler, "SignatureEncodingProcessor.invalid");

    Flux.from(source).publishOn(scheduler.toReactor()).subscribe(this::hookOnNext);
  }

  private void hookOnNext(ReceivedAttestation attestation) {
    if (checker.check(attestation)) {
      valid.onNext(attestation);
    } else {
      invalid.onNext(attestation);
    }
  }

  public Publisher<ReceivedAttestation> getValid() {
    return valid;
  }

  public Publisher<ReceivedAttestation> getInvalid() {
    return invalid;
  }
}
