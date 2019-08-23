package org.ethereum.beacon.chain.pool;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class InMemoryAttestationPool implements AttestationPool {

  private final Publisher<ReceivedAttestation> inbound;
  private final AttestationVerifier lightVerifier;
  private final AttestationProcessor processedAttestationPool;
  private final UnknownBlockPool unknownBlockPool;
  private final AttestationVerifier fullVerifier;
  private final AttestationChurn churn;

  public InMemoryAttestationPool(
      Publisher<ReceivedAttestation> inbound,
      AttestationVerifier lightVerifier,
      AttestationProcessor processedAttestationPool,
      UnknownBlockPool unknownBlockPool,
      AttestationVerifier fullVerifier,
      AttestationChurn churn) {
    this.inbound = inbound;
    this.lightVerifier = lightVerifier;
    this.processedAttestationPool = processedAttestationPool;
    this.unknownBlockPool = unknownBlockPool;
    this.fullVerifier = fullVerifier;
    this.churn = churn;
  }

  @Override
  public void start() {
    Flux.from(inbound).subscribe(lightVerifier::in);
    Flux.from(lightVerifier.out()).subscribe(processedAttestationPool::in);
    Flux.from(processedAttestationPool.out()).subscribe(unknownBlockPool::in);
    Flux.from(unknownBlockPool.out())
        .bufferTimeout(VERIFIER_BUFFER_SIZE, VERIFIER_INTERVAL)
        .subscribe(fullVerifier::batchIn);
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
