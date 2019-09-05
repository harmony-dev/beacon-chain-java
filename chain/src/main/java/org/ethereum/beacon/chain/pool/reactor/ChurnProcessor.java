package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.pool.AttestationPool;
import org.ethereum.beacon.chain.pool.OffChainAggregates;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.churn.AttestationChurn;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.OutsourcePublisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

public class ChurnProcessor extends Flux<OffChainAggregates> {

  private final AttestationChurn churn;
  private final OutsourcePublisher<OffChainAggregates> out = new OutsourcePublisher<>();

  public ChurnProcessor(
      AttestationChurn churn,
      Schedulers schedulers,
      Flux<BeaconTuple> chainHeads,
      Flux<Checkpoint> justifiedCheckpoints,
      Flux<Checkpoint> finalizedCheckpoints,
      Flux<ReceivedAttestation> source) {
    this.churn = churn;

    Scheduler scheduler = schedulers.newSingleThreadDaemon("pool-attestation-churn").toReactor();
    chainHeads.publishOn(scheduler).subscribe(this::hookOnNext);
    justifiedCheckpoints.publishOn(scheduler).subscribe(this.churn::feedJustifiedCheckpoint);
    finalizedCheckpoints.publishOn(scheduler).subscribe(this.churn::feedFinalizedCheckpoint);
    source
        .publishOn(scheduler)
        .map(ReceivedAttestation::getMessage)
        .bufferTimeout(AttestationPool.VERIFIER_BUFFER_SIZE, AttestationPool.VERIFIER_INTERVAL)
        .subscribe(this.churn::add);
  }

  private void hookOnNext(BeaconTuple tuple) {
    if (churn.isInitialized()) {
      OffChainAggregates aggregates = churn.compute(tuple);
      out.publishOut(aggregates);
    }
  }

  @Override
  public void subscribe(CoreSubscriber<? super OffChainAggregates> actual) {
    out.subscribe(actual);
  }
}
