package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.registry.ProcessedAttestations;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.OutsourcePublisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

/**
 * Passes attestations through {@link ProcessedAttestations} filter.
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
 *   <li>Not yet processed attestations
 * </ul>
 */
public class DoubleWorkProcessor extends Flux<ReceivedAttestation> {

  private final ProcessedAttestations processedAttestations;
  private final OutsourcePublisher<ReceivedAttestation> out = new OutsourcePublisher<>();

  public DoubleWorkProcessor(
      ProcessedAttestations processedAttestations,
      Schedulers schedulers,
      Flux<ReceivedAttestation> source) {
    this.processedAttestations = processedAttestations;

    Scheduler scheduler = schedulers.newSingleThreadDaemon("pool-double-work").toReactor();
    source.publishOn(scheduler).subscribe(this::hookOnNext);
  }

  private void hookOnNext(ReceivedAttestation attestation) {
    if (!processedAttestations.add(attestation)) {
      out.publishOut(attestation);
    }
  }

  @Override
  public void subscribe(CoreSubscriber<? super ReceivedAttestation> actual) {
    out.subscribe(actual);
  }
}
