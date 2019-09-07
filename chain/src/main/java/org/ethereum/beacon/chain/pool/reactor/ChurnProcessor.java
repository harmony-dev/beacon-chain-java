package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.pool.AttestationPool;
import org.ethereum.beacon.chain.pool.OffChainAggregates;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.churn.AttestationChurn;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

public class ChurnProcessor extends Flux<OffChainAggregates> {

  private final AttestationChurn churn;
  private final SimpleProcessor<OffChainAggregates> out;

  public ChurnProcessor(
      AttestationChurn churn,
      Schedulers schedulers,
      Publisher<SlotNumber> newSlots,
      Publisher<BeaconTuple> chainHeads,
      Publisher<Checkpoint> justifiedCheckpoints,
      Publisher<Checkpoint> finalizedCheckpoints,
      Publisher<ReceivedAttestation> source) {
    this.churn = churn;

    Scheduler scheduler = schedulers.newSingleThreadDaemon("pool-churn-processor").toReactor();
    Flux.from(chainHeads).publishOn(scheduler).subscribe(this::hookOnNext);
    Flux.from(newSlots).publishOn(scheduler).subscribe(this.churn::feedNewSlot);
    Flux.from(justifiedCheckpoints)
        .publishOn(scheduler)
        .subscribe(this.churn::feedJustifiedCheckpoint);
    Flux.from(finalizedCheckpoints)
        .publishOn(scheduler)
        .subscribe(this.churn::feedFinalizedCheckpoint);
    Flux.from(source)
        .publishOn(scheduler)
        .map(ReceivedAttestation::getMessage)
        .bufferTimeout(
            AttestationPool.VERIFIER_BUFFER_SIZE, AttestationPool.VERIFIER_INTERVAL, scheduler)
        .subscribe(this.churn::add);

    out = new SimpleProcessor<>(scheduler, "ChurnProcessor.out");
  }

  private void hookOnNext(BeaconTuple tuple) {
    if (churn.isInitialized()) {
      OffChainAggregates aggregates = churn.compute(tuple);
      out.onNext(aggregates);
    }
  }

  @Override
  public void subscribe(CoreSubscriber<? super OffChainAggregates> actual) {
    out.subscribe(actual);
  }
}
