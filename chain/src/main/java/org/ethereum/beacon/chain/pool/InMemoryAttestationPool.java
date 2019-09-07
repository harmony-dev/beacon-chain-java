package org.ethereum.beacon.chain.pool;

import java.util.List;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.pool.checker.SanityChecker;
import org.ethereum.beacon.chain.pool.checker.SignatureEncodingChecker;
import org.ethereum.beacon.chain.pool.checker.TimeFrameFilter;
import org.ethereum.beacon.chain.pool.churn.AttestationChurn;
import org.ethereum.beacon.chain.pool.reactor.ChurnProcessor;
import org.ethereum.beacon.chain.pool.reactor.DoubleWorkProcessor;
import org.ethereum.beacon.chain.pool.reactor.IdentificationProcessor;
import org.ethereum.beacon.chain.pool.reactor.SanityProcessor;
import org.ethereum.beacon.chain.pool.reactor.SignatureEncodingProcessor;
import org.ethereum.beacon.chain.pool.reactor.TimeProcessor;
import org.ethereum.beacon.chain.pool.reactor.VerificationProcessor;
import org.ethereum.beacon.chain.pool.registry.ProcessedAttestations;
import org.ethereum.beacon.chain.pool.registry.UnknownAttestationPool;
import org.ethereum.beacon.chain.pool.verifier.BatchVerifier;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

/**
 * An implementation of attestation pool based on <a href="https://projectreactor.io/">Reactor</a>
 * library, one of the implementation of reactive streams.
 */
public class InMemoryAttestationPool implements AttestationPool {

  private final Publisher<ReceivedAttestation> source;
  private final Publisher<SlotNumber> newSlots;
  private final Publisher<Checkpoint> justifiedCheckpoints;
  private final Publisher<Checkpoint> finalizedCheckpoints;
  private final Publisher<BeaconBlock> importedBlocks;
  private final Publisher<BeaconTuple> chainHeads;
  private final Schedulers schedulers;

  private final TimeFrameFilter timeFrameFilter;
  private final SanityChecker sanityChecker;
  private final SignatureEncodingChecker encodingChecker;
  private final ProcessedAttestations processedFilter;
  private final UnknownAttestationPool unknownPool;
  private final BatchVerifier verifier;
  private final AttestationChurn attestationChurn;

  private final DirectProcessor<ReceivedAttestation> invalidAttestations = DirectProcessor.create();
  private final DirectProcessor<ReceivedAttestation> validAttestations = DirectProcessor.create();
  private final DirectProcessor<ReceivedAttestation> unknownAttestations = DirectProcessor.create();
  private final DirectProcessor<OffChainAggregates> offChainAggregates = DirectProcessor.create();

  public InMemoryAttestationPool(
      Publisher<ReceivedAttestation> source,
      Publisher<SlotNumber> newSlots,
      Publisher<Checkpoint> justifiedCheckpoints,
      Publisher<Checkpoint> finalizedCheckpoints,
      Publisher<BeaconBlock> importedBlocks,
      Publisher<BeaconTuple> chainHeads,
      Schedulers schedulers,
      TimeFrameFilter timeFrameFilter,
      SanityChecker sanityChecker,
      SignatureEncodingChecker encodingChecker,
      ProcessedAttestations processedFilter,
      UnknownAttestationPool unknownPool,
      BatchVerifier batchVerifier,
      AttestationChurn attestationChurn) {
    this.source = source;
    this.newSlots = newSlots;
    this.justifiedCheckpoints = justifiedCheckpoints;
    this.finalizedCheckpoints = finalizedCheckpoints;
    this.importedBlocks = importedBlocks;
    this.chainHeads = chainHeads;
    this.schedulers = schedulers;
    this.timeFrameFilter = timeFrameFilter;
    this.sanityChecker = sanityChecker;
    this.encodingChecker = encodingChecker;
    this.processedFilter = processedFilter;
    this.unknownPool = unknownPool;
    this.verifier = batchVerifier;
    this.attestationChurn = attestationChurn;
  }

  @Override
  public void start() {
    org.ethereum.beacon.schedulers.Scheduler parallelExecutor =
        schedulers.newParallelDaemon("attestation-pool-%d", AttestationPool.MAX_THREADS);

    // create sources
    Flux<ReceivedAttestation> sourceFx = Flux.from(source);
    Flux<SlotNumber> newSlotsFx = Flux.from(newSlots);
    Flux<Checkpoint> justifiedCheckpointsFx = Flux.from(justifiedCheckpoints);
    Flux<Checkpoint> finalizedCheckpointsFx = Flux.from(finalizedCheckpoints);
    Flux<BeaconBlock> importedBlocksFx = Flux.from(importedBlocks);
    Flux<BeaconTuple> chainHeadsFx = Flux.from(chainHeads);

    // check time frames
    TimeProcessor timeProcessor =
        new TimeProcessor(
            timeFrameFilter, schedulers, sourceFx, finalizedCheckpointsFx, newSlotsFx);

    // run sanity check
    SanityProcessor sanityProcessor =
        new SanityProcessor(sanityChecker, schedulers, timeProcessor, finalizedCheckpointsFx);

    // discard already processed attestations
    DoubleWorkProcessor doubleWorkProcessor =
        new DoubleWorkProcessor(processedFilter, schedulers, sanityProcessor.getValid());

    // check signature encoding
    SignatureEncodingProcessor encodingProcessor =
        new SignatureEncodingProcessor(encodingChecker, parallelExecutor, doubleWorkProcessor);

    // identify attestation target
    IdentificationProcessor identificationProcessor =
        new IdentificationProcessor(
            unknownPool, schedulers, encodingProcessor.getValid(), newSlotsFx, importedBlocksFx);

    // verify attestations
    Flux<List<ReceivedAttestation>> verificationThrottle =
        Flux.from(identificationProcessor.getIdentified())
            .publishOn(parallelExecutor.toReactor())
            .bufferTimeout(VERIFIER_BUFFER_SIZE, VERIFIER_INTERVAL, parallelExecutor.toReactor());
    VerificationProcessor verificationProcessor =
        new VerificationProcessor(verifier, parallelExecutor, verificationThrottle);

    // feed churn
    ChurnProcessor churnProcessor =
        new ChurnProcessor(
            attestationChurn,
            schedulers,
            newSlotsFx,
            chainHeadsFx,
            justifiedCheckpointsFx,
            finalizedCheckpointsFx,
            verificationProcessor.getValid());

    Scheduler outScheduler = schedulers.events().toReactor();
    // expose valid attestations
    Flux.from(verificationProcessor.getValid())
        .publishOn(outScheduler)
        .subscribe(validAttestations);
    // expose not yet identified
    Flux.from(identificationProcessor.getUnknown())
        .publishOn(outScheduler)
        .subscribe(unknownAttestations);
    // expose aggregates
    churnProcessor.publishOn(outScheduler).subscribe(offChainAggregates);
    // expose invalid attestations
    Flux.merge(
            sanityProcessor.getInvalid(),
            encodingProcessor.getInvalid(),
            verificationProcessor.getInvalid())
        .publishOn(outScheduler)
        .subscribe(invalidAttestations);
  }

  @Override
  public Publisher<ReceivedAttestation> getValid() {
    return validAttestations;
  }

  @Override
  public Publisher<ReceivedAttestation> getInvalid() {
    return invalidAttestations;
  }

  @Override
  public Publisher<ReceivedAttestation> getUnknownAttestations() {
    return unknownAttestations;
  }

  @Override
  public Publisher<OffChainAggregates> getAggregates() {
    return offChainAggregates;
  }
}
