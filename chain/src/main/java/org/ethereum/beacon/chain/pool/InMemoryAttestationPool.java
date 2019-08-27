package org.ethereum.beacon.chain.pool;

import org.ethereum.beacon.chain.pool.checker.SignatureEncodingChecker;
import org.ethereum.beacon.chain.pool.churn.OffChainAggregates;
import org.ethereum.beacon.chain.pool.reactor.AttestationChurnProcessor;
import org.ethereum.beacon.chain.pool.reactor.AttestationVerificationProcessor;
import org.ethereum.beacon.chain.pool.reactor.IdentifyProcessor;
import org.ethereum.beacon.chain.pool.reactor.Input;
import org.ethereum.beacon.chain.pool.reactor.SanityCheckProcessor;
import org.ethereum.beacon.chain.pool.registry.ProcessedAttestations;
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

  private final SanityCheckProcessor sanityChecker;
  private final SignatureEncodingChecker encodingChecker;
  private final ProcessedAttestations processedFilter;
  private final IdentifyProcessor identifier;
  private final AttestationVerificationProcessor verifier;
  private final AttestationChurnProcessor churn;

  private final DirectProcessor<ReceivedAttestation> invalidAttestations = DirectProcessor.create();
  private final DirectProcessor<ReceivedAttestation> validAttestations = DirectProcessor.create();

  public InMemoryAttestationPool(
      Publisher<ReceivedAttestation> source,
      Publisher<SlotNumber> newSlots,
      Publisher<Checkpoint> finalizedCheckpoints,
      Publisher<BeaconBlock> importedBlocks,
      Publisher<BeaconBlock> chainHeads,
      Schedulers schedulers,
      SanityCheckProcessor sanityChecker,
      SignatureEncodingChecker encodingChecker,
      ProcessedAttestations processedFilter,
      IdentifyProcessor identifier,
      AttestationVerificationProcessor verifier,
      AttestationChurnProcessor churn) {
    this.source = source;
    this.newSlots = newSlots;
    this.finalizedCheckpoints = finalizedCheckpoints;
    this.importedBlocks = importedBlocks;
    this.chainHeads = chainHeads;
    this.schedulers = schedulers;
    this.sanityChecker = sanityChecker;
    this.encodingChecker = encodingChecker;
    this.processedFilter = processedFilter;
    this.identifier = identifier;
    this.verifier = verifier;
    this.churn = churn;
  }

  @Override
  public void start() {
    Scheduler executor =
        schedulers.newParallelDaemon("attestation-pool-%d", AttestationPool.MAX_THREADS);

    Flux<Input> sourceFx = Flux.from(source).map(Input::wrap).publishOn(executor.toReactor());
    Flux<Input> newSlotsFx = Flux.from(newSlots).map(Input::wrap).publishOn(executor.toReactor());
    Flux<Input> importedBlocksFx =
        Flux.from(importedBlocks).map(Input::wrap).publishOn(executor.toReactor());
    Flux<Input> finalizedCheckpointsFx =
        Flux.from(finalizedCheckpoints).map(Input::wrap).publishOn(executor.toReactor());
    Flux<Input> chainHeadsFx =
        Flux.from(chainHeads).map(Input::wrap).publishOn(executor.toReactor());

    // subscribe sanity checker to its inputs
    Flux.merge(sourceFx, newSlotsFx, finalizedCheckpointsFx).subscribe(sanityChecker);
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
    Flux.merge(encodingCheckOut.getSatisfied().map(Input::wrap), newSlotsFx, importedBlocksFx)
        .subscribe(identifier);

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
