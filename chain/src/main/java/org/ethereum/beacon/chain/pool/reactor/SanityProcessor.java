package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.SanityChecker;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.OutsourcePublisher;
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

  private final OutsourcePublisher<ReceivedAttestation> valid = new OutsourcePublisher<>();
  private final OutsourcePublisher<ReceivedAttestation> invalid = new OutsourcePublisher<>();

  public SanityProcessor(
      SanityChecker checker,
      Schedulers schedulers,
      Flux<ReceivedAttestation> source,
      Flux<Checkpoint> finalizedCheckpoints) {
    this.checker = checker;

    Scheduler scheduler = schedulers.newSingleThreadDaemon("pool-sanity-checker").toReactor();
    finalizedCheckpoints.publishOn(scheduler).subscribe(this.checker::feedFinalizedCheckpoint);
    source.publishOn(scheduler).subscribe(this::hookOnNext);
  }

  private void hookOnNext(ReceivedAttestation attestation) {
    if (!checker.isInitialized()) {
      return;
    }

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
