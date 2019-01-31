package org.ethereum.beacon.chain.observer;

import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.LMDGhostHeadFunction;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.HeadFunction;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;

public class ObservableStateProcessorImpl implements ObservableStateProcessor {

  BeaconChainStorage chainStorage;
  BeaconTupleStorage tupleStorage;
  ObservableBeaconState observableState;
  BeaconChainHead head;

  Publisher<BeaconState> slotStatesStream;
  BeaconState latestState;
  PendingOperations latestPendingOperations;
  SpecHelpers specHelpers;

  private final ReplayProcessor<BeaconChainHead> headSink = ReplayProcessor.cacheLast();
  private final Publisher<BeaconChainHead> headStream =
      Flux.from(headSink)
          .publishOn(Schedulers.single())
          .onBackpressureError()
          .name("ObservableStateProcessor.head");
  private final ReplayProcessor<ObservableBeaconState> observableStateSink =
      ReplayProcessor.cacheLast();
  private final Publisher<ObservableBeaconState> observableStateStream =
      Flux.from(observableStateSink)
          .publishOn(Schedulers.single())
          .onBackpressureError()
          .name("ObservableStateProcessor.observableState");

  public ObservableStateProcessorImpl(
      BeaconChainStorage chainStorage,
      Publisher<BeaconState> slotStatesStream,
      Publisher<PendingOperations> pendingOperationsPublisher,
      SpecHelpers specHelpers) {
    this.chainStorage = chainStorage;
    this.tupleStorage = chainStorage.getBeaconTupleStorage();
    this.slotStatesStream = slotStatesStream;
    this.specHelpers = specHelpers;
    Flux.from(slotStatesStream).doOnNext(this::onSlotStateUpdate).subscribe();
    Flux.from(pendingOperationsPublisher).doOnNext(this::onPendingStateUpdate).subscribe();
  }

  private void onSlotStateUpdate(BeaconState slotState) {
    this.latestState = slotState;
  }

  private void onPendingStateUpdate(PendingOperations pendingOperations) {
    this.latestPendingOperations = pendingOperations;
    updateObservableState();
  }

  private void updateObservableState() {
    updateHead();
    ObservableBeaconState newObservableState =
        new ObservableBeaconState(head.getBlock(), latestState, latestPendingOperations);
    if (!newObservableState.equals(observableState)) {
      this.observableState = newObservableState;
      observableStateSink.onNext(newObservableState);
    }
  }

  private HeadFunction createLatestHeadFunction() {
    return new LMDGhostHeadFunction(
        chainStorage,
        validatorRecord -> latestPendingOperations.findAttestation(validatorRecord.getPubKey()),
        specHelpers);
  }

  private void updateHead() {
    HeadFunction headFunction = createLatestHeadFunction();
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

  @Override
  public Publisher<BeaconChainHead> getHeadStream() {
    return headStream;
  }

  @Override
  public Publisher<ObservableBeaconState> getObservableStateStream() {
    return observableStateStream;
  }
}
