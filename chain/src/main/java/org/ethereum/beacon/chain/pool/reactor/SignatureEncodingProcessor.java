package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.SignatureEncodingChecker;
import org.ethereum.beacon.stream.OutsourcePublisher;
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
  private final OutsourcePublisher<ReceivedAttestation> valid = new OutsourcePublisher<>();
  private final OutsourcePublisher<ReceivedAttestation> invalid = new OutsourcePublisher<>();

  public SignatureEncodingProcessor(
      SignatureEncodingChecker checker, Flux<ReceivedAttestation> source) {
    this.checker = checker;

    source.subscribe(this::hookOnNext);
  }

  private void hookOnNext(ReceivedAttestation attestation) {
    if (checker.check(attestation)) {
      valid.publishOut(attestation);
    } else {
      invalid.publishOut(attestation);
    }
  }

  public Flux<ReceivedAttestation> getValid() {
    return valid;
  }

  public Flux<ReceivedAttestation> getInvalid() {
    return invalid;
  }
}
