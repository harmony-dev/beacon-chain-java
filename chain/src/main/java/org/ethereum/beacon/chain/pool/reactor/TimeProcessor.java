package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.TimeFrameFilter;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

/**
 * Passes attestations through a {@link TimeFrameFilter}.
 *
 * <p>Input:
 *
 * <ul>
 *   <li>recently finalized checkpoints
 *   <li>new slots
 *   <li>attestations
 * </ul>
 *
 * <p>Output:
 *
 * <ul>
 *   <li>attestations satisfying time frames
 * </ul>
 */
public class TimeProcessor extends Flux<ReceivedAttestation> {

  private final TimeFrameFilter filter;
  private final SimpleProcessor<ReceivedAttestation> out;

  public TimeProcessor(
      TimeFrameFilter filter,
      Schedulers schedulers,
      Publisher<ReceivedAttestation> source,
      Publisher<Checkpoint> finalizedCheckpoints,
      Publisher<SlotNumber> newSlots) {
    this.filter = filter;

    Scheduler scheduler = schedulers.newSingleThreadDaemon("pool-time-processor").toReactor();
    out = new SimpleProcessor<>(scheduler, "TimeProcessor.out");
    Flux.from(source).publishOn(scheduler).subscribe(this::hookOnNext);
    Flux.from(finalizedCheckpoints)
        .publishOn(scheduler)
        .subscribe(this.filter::feedFinalizedCheckpoint);
    Flux.from(newSlots).publishOn(scheduler).subscribe(this.filter::feedNewSlot);
  }

  private void hookOnNext(ReceivedAttestation attestation) {
    if (filter.isInitialized() && filter.check(attestation)) {
      out.onNext(attestation);
    }
  }

  @Override
  public void subscribe(CoreSubscriber<? super ReceivedAttestation> actual) {
    out.subscribe(actual);
  }
}
