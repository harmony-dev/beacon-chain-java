package org.ethereum.beacon.chain;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.ScoreFunction;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.types.Score;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.db.Database;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;

public class DefaultBeaconChain implements MutableBeaconChain {

  ChainSpec chainSpec;

  BeaconBlockStorage blockStorage;
  BeaconStateStorage stateStorage;
  BeaconTupleStorage tupleStorage;

  StateTransition<BeaconState> initialTransition;
  StateTransition<BeaconState> slotTransition;
  StateTransition<BeaconState> stateTransition;

  BeaconBlockVerifier blockVerifier;
  BeaconStateVerifier stateVerifier;

  ScoreFunction scoreFunction;

  Database database;

  BeaconChainHead head;

  private final ReplayProcessor<BeaconChainHead> headSink = ReplayProcessor.cacheLast();
  private final Publisher<BeaconChainHead> headStream = Flux.from(headSink)
      .publishOn(Schedulers.single())
      .onBackpressureError()
      .name("DefaultBeaconChain.head");
  private final ReplayProcessor<BeaconTuple> blockSink = ReplayProcessor.cacheLast();
  private final Publisher<BeaconTuple> blockStream = Flux.from(blockSink)
      .publishOn(Schedulers.single())
      .onBackpressureError()
      .name("DefaultBeaconChain.block");
  private final ReplayProcessor<BeaconState> slotSink = ReplayProcessor.cacheLast();
  private final Publisher<BeaconState> slotStream = Flux.from(slotSink)
      .doOnSubscribe(s -> slotStreamSubscribersUpdate())
      .doOnCancel(() -> slotStreamSubscribersUpdate())
      .publishOn(Schedulers.single())
      .onBackpressureError()
      .name("DefaultBeaconChain.slot");
  private ScheduledFuture<Void> stateTrackingTask;


  @Override
  public void init() {
    if (blockStorage.isEmpty()) {
      initializeStorage();
    }
    BeaconTuple headTuple = tupleStorage.getCanonicalHead();
    blockSink.onNext(headTuple);

    Score headScore = scoreFunction.apply(headTuple.getBlock(), headTuple.getState());
    this.head = BeaconChainHead.of(headTuple, headScore);
    headSink.onNext(this.head);
  }

  private BeaconTuple initializeStorage() {
    BeaconState initialState =
        initialTransition.apply(BeaconBlocks.createGenesis(chainSpec), BeaconState.getEmpty());
    BeaconBlock genesis =
        BeaconBlocks.createGenesis(chainSpec).withStateRoot(initialState.getHash());

    BeaconTuple tuple = BeaconTuple.of(genesis, initialState);
    tupleStorage.put(tuple);

    return tuple;
  }

  private void slotStreamSubscribersUpdate() {
    if (slotSink.downstreamCount() > 0) {
      stateTrackingTask = startSlotStateTracking();
    } else {
      stateTrackingTask.cancel(false);
    }
  }

  private ScheduledFuture<Void> startSlotStateTracking() {
    // TODO
    return null;
  }

  @Override
  public synchronized void insert(BeaconBlock block) {
    assert head != null;

    if (exist(block)) {
      return;
    }

    if (!hasParent(block)) {
      return;
    }

    BeaconState parentState = pullParentState(block);
    BeaconState slotTransitedState = slotTransition.apply(block, parentState);

    VerificationResult blockVerification = blockVerifier.verify(block, slotTransitedState);
    if (!blockVerification.isPassed()) {
      return;
    }

    BeaconState newState = stateTransition.apply(block, slotTransitedState);

    VerificationResult stateVerification = stateVerifier.verify(newState, block);
    if (stateVerification.isPassed()) {
      return;
    }

    BeaconTuple newTuple = BeaconTuple.of(block, newState);
    tupleStorage.put(newTuple);

    Score newScore = scoreFunction.apply(block, newState);
    if (head.getScore().compareTo(newScore) < 0) {
      blockStorage.reorgTo(newTuple.getBlock().getHash());
      this.head = BeaconChainHead.of(newTuple, newScore);
      headSink.onNext(this.head);
    }

    database.commit();

    blockSink.onNext(newTuple);
  }

  private BeaconState pullParentState(BeaconBlock block) {
    if (head.getBlock().isParentOf(block)) {
      return head.getState();
    } else {
      Optional<BeaconState> parentState = stateStorage.get(block.getParentRoot());
      checkArgument(parentState.isPresent(), "No parent for block %s", block);
      return parentState.get();
    }
  }

  private boolean exist(BeaconBlock block) {
    return blockStorage.get(block.getHash()).isPresent();
  }

  private boolean hasParent(BeaconBlock block) {
    return blockStorage.get(block.getParentRoot()).isPresent();
  }

  @Override
  public Publisher<BeaconChainHead> getHeadStream() {
    return headStream;
  }

  @Override
  public Publisher<BeaconTuple> getBlockStatesStream() {
    return blockStream;
  }

  @Override
  public Publisher<BeaconState> getSlotStatesStream() {
    return slotStream;
  }
}
