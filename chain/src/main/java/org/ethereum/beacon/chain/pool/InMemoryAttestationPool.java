package org.ethereum.beacon.chain.pool;

import org.ethereum.beacon.chain.pool.checker.SanityChecker;
import org.ethereum.beacon.chain.pool.checker.SignatureEncodingChecker;
import org.ethereum.beacon.chain.pool.checker.TimeFrameFilter;
import org.ethereum.beacon.chain.pool.churn.OffChainAggregates;
import org.ethereum.beacon.chain.pool.reactor.ChurnProcessor;
import org.ethereum.beacon.chain.pool.reactor.VerificationProcessor;
import org.ethereum.beacon.chain.pool.reactor.IdentificationProcessor;
import org.ethereum.beacon.chain.pool.reactor.SanityProcessor;
import org.ethereum.beacon.chain.pool.reactor.TimeProcessor;
import org.ethereum.beacon.chain.pool.registry.ProcessedAttestations;
import org.ethereum.beacon.chain.pool.registry.UnknownAttestationPool;
import org.ethereum.beacon.chain.pool.verifier.BatchVerifier;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.Fluxes;
import org.ethereum.beacon.stream.Fluxes.FluxSplit;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;

public class InMemoryAttestationPool implements AttestationPool {

  private final Publisher<ReceivedAttestation> source;
  private final Publisher<SlotNumber> newSlots;
  private final Publisher<Checkpoint> finalizedCheckpoints;
  private final Publisher<BeaconBlock> importedBlocks;
  private final Publisher<BeaconBlock> chainHeads;
  private final Schedulers schedulers;

  private final TimeProcessor timeProcessor;
  private final SanityProcessor sanityChecker;
  private final SignatureEncodingChecker encodingChecker;
  private final ProcessedAttestations processedFilter;
  private final IdentificationProcessor identifier;
  private final VerificationProcessor verifier;
  private final ChurnProcessor churn;

  private final DirectProcessor<ReceivedAttestation> invalidAttestations = DirectProcessor.create();
  private final DirectProcessor<ReceivedAttestation> validAttestations = DirectProcessor.create();

  public InMemoryAttestationPool(
      Publisher<ReceivedAttestation> source,
      Publisher<SlotNumber> newSlots,
      Publisher<Checkpoint> finalizedCheckpoints,
      Publisher<BeaconBlock> importedBlocks,
      Publisher<BeaconBlock> chainHeads,
      Schedulers schedulers,
      TimeFrameFilter timeFrameFilter,
      SanityChecker sanityChecker,
      SignatureEncodingChecker encodingChecker,
      ProcessedAttestations processedFilter,
      UnknownAttestationPool unknownAttestationPool,
      BatchVerifier batchVerifier) {
    this.source = source;
    this.newSlots = newSlots;
    this.finalizedCheckpoints = finalizedCheckpoints;
    this.importedBlocks = importedBlocks;
    this.chainHeads = chainHeads;
    this.schedulers = schedulers;
    this.timeProcessor = new TimeProcessor(timeFrameFilter);
    this.sanityChecker = new SanityProcessor(sanityChecker);
    this.encodingChecker = encodingChecker;
    this.processedFilter = processedFilter;
    this.identifier = new IdentificationProcessor(unknownAttestationPool);
    this.verifier = new VerificationProcessor(batchVerifier);
    this.churn = new ChurnProcessor();
  }

  @Override
  public void start() {
    Scheduler executor =
        schedulers.newParallelDaemon("attestation-pool-%d", AttestationPool.MAX_THREADS);

    Flux<?> sourceFx = Flux.from(source).publishOn(executor.toReactor());
    Flux<?> newSlotsFx = Flux.from(newSlots).publishOn(executor.toReactor());
    Flux<?> importedBlocksFx = Flux.from(importedBlocks).publishOn(executor.toReactor());
    Flux<?> finalizedCheckpointsFx =
        Flux.from(finalizedCheckpoints).publishOn(executor.toReactor());
    Flux<?> chainHeadsFx = Flux.from(chainHeads).publishOn(executor.toReactor());

    // start from time frame processor
    Flux.merge(sourceFx, newSlotsFx, finalizedCheckpointsFx).subscribe(timeProcessor);
    FluxSplit<CheckedAttestation> timeFrameOut =
        Fluxes.split(timeProcessor, CheckedAttestation::isPassed);

    // subscribe sanity checker
    Flux.merge(
            timeFrameOut.getSatisfied().map(CheckedAttestation::getAttestation),
            finalizedCheckpointsFx)
        .subscribe(sanityChecker);
    FluxSplit<CheckedAttestation> sanityOut =
        Fluxes.split(sanityChecker, CheckedAttestation::isPassed);

    // filter already processed attestations
    Flux<ReceivedAttestation> newAttestations =
        sanityOut
            .getSatisfied()
            .map(CheckedAttestation::getAttestation)
            .filter(processedFilter::add);

    // check signature encoding
    FluxSplit<ReceivedAttestation> encodingCheckOut =
        Fluxes.split(newAttestations, encodingChecker::check);

    // identify attestation target
    Flux.merge(encodingCheckOut.getSatisfied(), newSlotsFx, importedBlocksFx).subscribe(identifier);

    // verify attestations
    identifier.bufferTimeout(VERIFIER_BUFFER_SIZE, VERIFIER_INTERVAL).subscribe(verifier);
    FluxSplit<CheckedAttestation> verifierOut =
        Fluxes.split(verifier, CheckedAttestation::isPassed);

    // feed churn
    Flux.merge(
        verifierOut.getSatisfied().map(CheckedAttestation::getAttestation),
        newSlotsFx,
        chainHeadsFx);

    // expose invalid attestations
    Flux.merge(
            sanityOut.getUnsatisfied().map(CheckedAttestation::getAttestation),
            encodingCheckOut.getUnsatisfied(),
            verifierOut.getUnsatisfied().map(CheckedAttestation::getAttestation))
        .subscribe(invalidAttestations);

    // expose valid attestations
    verifierOut.getSatisfied().map(CheckedAttestation::getAttestation).subscribe(validAttestations);
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
    return identifier.getUnknownAttestations();
  }

  @Override
  public Publisher<OffChainAggregates> getAggregates() {
    return churn;
  }
}
