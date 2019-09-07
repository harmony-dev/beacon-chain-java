package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.TimeFrameFilter;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
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
      Flux<ReceivedAttestation> source,
      Flux<Checkpoint> finalizedCheckpoints,
      Flux<SlotNumber> newSlots) {
    this.filter = filter;

    Scheduler scheduler = schedulers.newSingleThreadDaemon("pool-time-frame-filter").toReactor();
    out = new SimpleProcessor<>(scheduler, "pool-time-simple-processor");
    source.publishOn(scheduler).subscribe(this::hookOnNext);
    finalizedCheckpoints.publishOn(scheduler).subscribe(this.filter::feedFinalizedCheckpoint);
    newSlots.publishOn(scheduler).subscribe(this.filter::feedNewSlot);
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
