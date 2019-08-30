package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.registry.UnknownAttestationPool;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.OutsourcePublisher;
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
  private final OutsourcePublisher<ReceivedAttestation> identified = new OutsourcePublisher<>();
  private final OutsourcePublisher<ReceivedAttestation> unknown = new OutsourcePublisher<>();

  public IdentificationProcessor(
      UnknownAttestationPool pool,
      Schedulers schedulers,
      Flux<ReceivedAttestation> source,
      Flux<SlotNumber> newSlots,
      Flux<BeaconBlock> newImportedBlocks) {
    this.pool = pool;

    Scheduler scheduler =
        schedulers.newSingleThreadDaemon("pool-attestation-identifier").toReactor();
    newSlots.publishOn(scheduler).subscribe(this.pool::feedNewSlot);
    newImportedBlocks.publishOn(scheduler).subscribe(this::hookOnNext);
    source.publishOn(scheduler).subscribe(this::hookOnNext);
  }

  private void hookOnNext(BeaconBlock block) {
    pool.feedNewImportedBlock(block).forEach(identified::publishOut);
  }

  private void hookOnNext(ReceivedAttestation attestation) {
    if (pool.isInitialized()) {
      if (pool.add(attestation)) {
        unknown.publishOut(attestation);
      } else {
        identified.publishOut(attestation);
      }
    }
  }

  public OutsourcePublisher<ReceivedAttestation> getIdentified() {
    return identified;
  }

  public OutsourcePublisher<ReceivedAttestation> getUnknown() {
    return unknown;
  }
}
