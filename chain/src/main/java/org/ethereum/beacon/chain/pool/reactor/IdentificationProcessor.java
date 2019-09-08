package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.registry.UnknownAttestationPool;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

/**
 * Passes attestations through {@link UnknownAttestationPool}.
 *
 * <p>Input:
 *
 * <ul>
 *   <li>newly imported blocks
 *   <li>new slots
 *   <li>attestations
 * </ul>
 *
 * <p>Output:
 *
 * <ul>
 *   <li>instantly identified attestations
 *   <li>attestations identified upon a new block come
 *   <li>attestations with not yet imported block
 * </ul>
 */
public class IdentificationProcessor {

  private final UnknownAttestationPool pool;
  private final SimpleProcessor<ReceivedAttestation> identified;
  private final SimpleProcessor<ReceivedAttestation> unknown;

  public IdentificationProcessor(
      UnknownAttestationPool pool,
      Schedulers schedulers,
      Publisher<ReceivedAttestation> source,
      Publisher<SlotNumber> newSlots,
      Publisher<BeaconBlock> newImportedBlocks) {
    this.pool = pool;

    Scheduler scheduler =
        schedulers.newSingleThreadDaemon("pool-identification-processor").toReactor();
    this.identified = new SimpleProcessor<>(scheduler, "IdentificationProcessor.identified");
    this.unknown = new SimpleProcessor<>(scheduler, "IdentificationProcessor.unknown");

    Flux.from(newSlots).publishOn(scheduler).subscribe(this.pool::feedNewSlot);
    Flux.from(newImportedBlocks).publishOn(scheduler).subscribe(this::hookOnNext);
    Flux.from(source).publishOn(scheduler).subscribe(this::hookOnNext);
  }

  private void hookOnNext(BeaconBlock block) {
    pool.feedNewImportedBlock(block).forEach(identified::onNext);
  }

  private void hookOnNext(ReceivedAttestation attestation) {
    if (pool.isInitialized()) {
      if (pool.add(attestation)) {
        unknown.onNext(attestation);
      } else {
        identified.onNext(attestation);
      }
    }
  }

  public Publisher<ReceivedAttestation> getIdentified() {
    return identified;
  }

  public Publisher<ReceivedAttestation> getUnknown() {
    return unknown;
  }
}
