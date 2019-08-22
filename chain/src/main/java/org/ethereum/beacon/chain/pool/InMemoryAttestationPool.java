package org.ethereum.beacon.chain.pool;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class InMemoryAttestationPool implements AttestationPool {

  private final Publisher<ReceivedAttestation> inbound;
  private final AttestationVerifier lightVerifier;
  private final AttestationProcessor knownAttestations;
  private final UnknownBlockPool unknownBlockPool;
  private final AttestationVerifier fullVerifier;
  private final AttestationChurn churn;

  public InMemoryAttestationPool(
      Publisher<ReceivedAttestation> inbound,
      AttestationVerifier lightVerifier,
      AttestationProcessor knownAttestations,
      UnknownBlockPool unknownBlockPool,
      AttestationVerifier fullVerifier,
      AttestationChurn churn) {
    this.inbound = inbound;
    this.lightVerifier = lightVerifier;
    this.knownAttestations = knownAttestations;
    this.unknownBlockPool = unknownBlockPool;
    this.fullVerifier = fullVerifier;
    this.churn = churn;
  }

  @Override
  public void start() {
    Flux.from(inbound).subscribe(lightVerifier::in);
    Flux.from(lightVerifier.out()).subscribe(knownAttestations::in);
    Flux.from(knownAttestations.out()).subscribe(unknownBlockPool::in);
    Flux.from(unknownBlockPool.out()).subscribe(fullVerifier::in);
    Flux.from(fullVerifier.out()).subscribe(churn::in);
  }

  @Override
  public Publisher<ReceivedAttestation> valid() {
    return fullVerifier.valid();
  }

  @Override
  public Publisher<ReceivedAttestation> invalid() {
    return Flux.concat(lightVerifier.invalid(), fullVerifier.invalid());
  }

  @Override
  public Publisher<ReceivedAttestation> unknownBlock() {
    return unknownBlockPool.unknownBlock();
  }

  @Override
  public Publisher<OffChainAggregates> aggregates() {
    return churn.out();
  }
}
