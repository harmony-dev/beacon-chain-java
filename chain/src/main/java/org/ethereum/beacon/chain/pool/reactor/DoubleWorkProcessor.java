package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.registry.ProcessedAttestations;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
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
  private final SimpleProcessor<ReceivedAttestation> out;

  public DoubleWorkProcessor(
      ProcessedAttestations processedAttestations,
      Schedulers schedulers,
      Publisher<ReceivedAttestation> source) {
    this.processedAttestations = processedAttestations;

    Scheduler scheduler =
        schedulers.newSingleThreadDaemon("pool-double-work-processor").toReactor();
    this.out = new SimpleProcessor<>(scheduler, "DoubleWorkProcessor.out");

    Flux.from(source).publishOn(scheduler).subscribe(this::hookOnNext);
  }

  private void hookOnNext(ReceivedAttestation attestation) {
    if (processedAttestations.add(attestation)) {
      out.onNext(attestation);
    }
  }

  @Override
  public void subscribe(CoreSubscriber<? super ReceivedAttestation> actual) {
    out.subscribe(actual);
  }
}
