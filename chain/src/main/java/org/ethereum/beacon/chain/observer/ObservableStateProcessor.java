package org.ethereum.beacon.chain.observer;

import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.HeadFunction;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;

public class ObservableStateProcessor {

  BeaconTupleStorage tupleStorage;
  ObservableBeaconState observableState;
  HeadFunction headFunction;
  BeaconChainHead head;

  Publisher<BeaconState> slotStatesStream;
  BeaconState latestState;

  private final ReplayProcessor<BeaconChainHead> headSink = ReplayProcessor.cacheLast();
  private final Publisher<BeaconChainHead> headStream = Flux.from(headSink)
      .publishOn(Schedulers.single())
      .onBackpressureError()
      .name("ObservableStateProcessor.head");
  private final ReplayProcessor<ObservableBeaconState> observableStateSink = ReplayProcessor.cacheLast();
  private final Publisher<ObservableBeaconState> observableStateStream = Flux.from(observableStateSink)
      .publishOn(Schedulers.single())
      .onBackpressureError()
      .name("ObservableStateProcessor.observableState");

  public ObservableStateProcessor(BeaconTupleStorage tupleStorage, HeadFunction headFunction,
                                  Publisher<BeaconState> slotStatesStream) {
    this.tupleStorage = tupleStorage;
    this.headFunction = headFunction;
    this.slotStatesStream = slotStatesStream;
    Flux.from(slotStatesStream)
        .doOnNext(this::onSlotStateUpdate)
        .subscribe();
  }

  private void onSlotStateUpdate(BeaconState slotState) {
    this.latestState = slotState;
    updateObservableState();
  }

  private void updateObservableState() {
    updateHead();
    ObservableBeaconState newObservableState = new ObservableBeaconState(head.getBlock(), latestState, observableState.getPendingOperations());
    if (!newObservableState.equals(observableState)) {
      this.observableState = newObservableState;
      observableStateSink.onNext(newObservableState);
    }
  }

  private void updateHead() {
    BeaconBlock newHead = headFunction.getHead();
    if (this.head != null && this.head.getBlock().equals(newHead)) {
      return; // == old
    }
    BeaconTuple newHeadTuple =
        tupleStorage
            .get(newHead.getHash())
            .orElseThrow(() -> new IllegalStateException("Beacon tuple not found for new head "));
    this.head = BeaconChainHead.of(newHeadTuple);

    headSink.onNext(this.head);
  }

  public Publisher<BeaconChainHead> getHeadStream() {
    return headStream;
  }

  public Publisher<ObservableBeaconState> getObservableStateStream() {
    return observableStateStream;
  }
}
