package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.SanityChecker;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

/**
 * Passes attestations through a {@link SanityChecker}.
 *
 * <p>Input:
 *
 * <ul>
 *   <li>recently finalized checkpoints
 *   <li>attestations
 * </ul>
 *
 * <p>Output:
 *
 * <ul>
 *   <li>attestations successfully passed sanity checks
 *   <li>invalid attestations
 * </ul>
 */
public class SanityProcessor {

  private final SanityChecker checker;

  private final SimpleProcessor<ReceivedAttestation> valid;
  private final SimpleProcessor<ReceivedAttestation> invalid;

  public SanityProcessor(
      SanityChecker checker,
      Schedulers schedulers,
      Publisher<ReceivedAttestation> source,
      Publisher<Checkpoint> finalizedCheckpoints) {
    this.checker = checker;

    Scheduler scheduler = schedulers.newSingleThreadDaemon("pool-sanity-processor").toReactor();

    Flux.from(finalizedCheckpoints)
        .publishOn(scheduler)
        .subscribe(this.checker::feedFinalizedCheckpoint);

    Flux.from(source)
            .publishOn(scheduler)
            .subscribe(this::hookOnNext);

    valid = new SimpleProcessor<>(scheduler, "SanityProcessor.valid");
    invalid = new SimpleProcessor<>(scheduler, "SanityProcessor.invalid");
  }

  private void hookOnNext(ReceivedAttestation attestation) {
    if (!checker.isInitialized()) {
      return;
    }

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
