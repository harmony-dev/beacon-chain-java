package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.churn.OffChainAggregates;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.OutsourcePublisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

public class ChurnProcessor extends Flux<OffChainAggregates> {

  private final OutsourcePublisher<OffChainAggregates> out = new OutsourcePublisher<>();

  public ChurnProcessor(
      Schedulers schedulers, Flux<BeaconBlock> chainHeads, Flux<SlotNumber> newSlots) {}

  protected void hookOnNext(ReceivedAttestation value) {
    // TODO implement
  }

  @Override
  public void subscribe(CoreSubscriber<? super OffChainAggregates> actual) {
    out.subscribe(actual);
  }
}
