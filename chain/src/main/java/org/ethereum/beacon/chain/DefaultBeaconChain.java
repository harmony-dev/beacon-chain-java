package org.ethereum.beacon.chain;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.db.Database;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class DefaultBeaconChain implements MutableBeaconChain {

  SpecHelpers specHelpers;
  ChainSpec chainSpec;

  BeaconBlockStorage blockStorage;
  BeaconStateStorage stateStorage;
  BeaconTupleStorage tupleStorage;

  StateTransition<BeaconState> initialTransition;
  StateTransition<BeaconState> slotTransition;
  StateTransition<BeaconState> stateTransition;

  BeaconBlockVerifier blockVerifier;
  BeaconStateVerifier stateVerifier;

  Database database;

  private final ReplayProcessor<BeaconTuple> blockSink = ReplayProcessor.cacheLast();
  private final Publisher<BeaconTuple> blockStream =
      Flux.from(blockSink)
          .publishOn(Schedulers.single())
          .onBackpressureError()
          .name("DefaultBeaconChain.block");

  @Override
  public void init() {
    if (blockStorage.isEmpty()) {
      initializeStorage();
    }
  }

  private BeaconTuple initializeStorage() {
    BeaconState initialState =
        initialTransition.apply(BeaconBlocks.createGenesis(chainSpec), BeaconState.getEmpty());
    BeaconBlock genesis =
        BeaconBlocks.createGenesis(chainSpec)
            .withStateRoot(hash(initialState));

    BeaconTuple tuple = BeaconTuple.of(genesis, initialState);
    tupleStorage.put(tuple);

    return tuple;
  }

  @Override
  public synchronized void insert(BeaconBlock block) {
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
    database.commit();

    blockSink.onNext(newTuple);
  }

  private BeaconState pullParentState(BeaconBlock block) {
    Optional<BeaconState> parentState = stateStorage.get(block.getParentRoot());
    checkArgument(parentState.isPresent(), "No parent for block %s", block);
    return parentState.get();
  }

  private boolean exist(BeaconBlock block) {
    return blockStorage.get(hash(block)).isPresent();
  }

  private boolean hasParent(BeaconBlock block) {
    return blockStorage.get(block.getParentRoot()).isPresent();
  }

  private Hash32 hash(Object object) {
    return specHelpers.hash_tree_root(object);
  }

  @Override
  public Publisher<BeaconTuple> getBlockStatesStream() {
    return blockStream;
  }
}
