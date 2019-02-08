package org.ethereum.beacon.chain;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.transition.BeaconStateEx;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.db.Database;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class DefaultBeaconChain implements MutableBeaconChain {

  private final SpecHelpers specHelpers;
  private final BlockTransition<BeaconStateEx> initialTransition;
  private final StateTransition<BeaconStateEx> perSlotTransition;
  private final BlockTransition<BeaconStateEx> perBlockTransition;
  private final StateTransition<BeaconStateEx> perEpochTransition;
  private final BeaconBlockVerifier blockVerifier;
  private final BeaconStateVerifier stateVerifier;

  private final BeaconTupleStorage tupleStorage;
  private final Database database;

  private final ReplayProcessor<BeaconTuple> blockSink = ReplayProcessor.cacheLast();
  private final Publisher<BeaconTuple> blockStream =
      Flux.from(blockSink)
          .publishOn(Schedulers.single())
          .onBackpressureError()
          .name("DefaultBeaconChain.block");

  public DefaultBeaconChain(
      SpecHelpers specHelpers,
      BlockTransition<BeaconStateEx> initialTransition,
      StateTransition<BeaconStateEx> perSlotTransition,
      BlockTransition<BeaconStateEx> perBlockTransition,
      StateTransition<BeaconStateEx> perEpochTransition,
      BeaconBlockVerifier blockVerifier,
      BeaconStateVerifier stateVerifier,
      BeaconTupleStorage tupleStorage,
      Database database) {
    this.specHelpers = specHelpers;
    this.initialTransition = initialTransition;
    this.perSlotTransition = perSlotTransition;
    this.perBlockTransition = perBlockTransition;
    this.perEpochTransition = perEpochTransition;
    this.blockVerifier = blockVerifier;
    this.stateVerifier = stateVerifier;
    this.tupleStorage = tupleStorage;
    this.database = database;
  }

  @Override
  public void init() {
    if (tupleStorage.isEmpty()) {
      initializeStorage();
    }
  }

  private void initializeStorage() {
    BeaconBlock initialGenesis = BeaconBlocks.createGenesis(specHelpers.getChainSpec());
    BeaconStateEx initialState =
        initialTransition.apply(
            new BeaconStateEx(BeaconState.getEmpty(), Hash32.ZERO), initialGenesis);

    Hash32 initialStateRoot = specHelpers.hash_tree_root(initialState.getCanonicalState());
    BeaconBlock genesis = initialGenesis.withStateRoot(initialStateRoot);
    BeaconTuple tuple = BeaconTuple.of(genesis, initialState);

    tupleStorage.put(tuple);
  }

  @Override
  public synchronized void insert(BeaconBlock block) {
    if (exist(block)) {
      return;
    }

    if (!hasParent(block)) {
      return;
    }

    BeaconStateEx parentState = pullParentState(block);

    BeaconStateEx preBlockState = applyEmptySlotTransitions(parentState, block);
    VerificationResult blockVerification =
        blockVerifier.verify(block, preBlockState.getCanonicalState());
    if (!blockVerification.isPassed()) {
      return;
    }

    BeaconStateEx postBlockState = perBlockTransition.apply(preBlockState, block);
    if (specHelpers.is_epoch_end(block.getSlot())) {
      postBlockState = perEpochTransition.apply(preBlockState);
    }

    VerificationResult stateVerification =
        stateVerifier.verify(postBlockState.getCanonicalState(), block);
    if (stateVerification.isPassed()) {
      return;
    }

    BeaconTuple newTuple = BeaconTuple.of(block, postBlockState);
    tupleStorage.put(newTuple);
    database.commit();

    blockSink.onNext(newTuple);
  }

  private BeaconStateEx pullParentState(BeaconBlock block) {
    Optional<BeaconTuple> parent = tupleStorage.get(block.getParentRoot());
    checkArgument(parent.isPresent(), "No parent for block %s", block);
    BeaconTuple parentTuple = parent.get();
    Hash32 parentHash = specHelpers.hash_tree_root(parentTuple.getBlock());

    return new BeaconStateEx(parentTuple.getState(), parentHash);
  }

  private boolean exist(BeaconBlock block) {
    Hash32 blockHash = specHelpers.hash_tree_root(block);
    return tupleStorage.get(blockHash).isPresent();
  }

  private boolean hasParent(BeaconBlock block) {
    return tupleStorage.get(block.getParentRoot()).isPresent();
  }

  private BeaconStateEx applyEmptySlotTransitions(BeaconStateEx source, BeaconBlock block) {
    BeaconStateEx result = source;
    while (result.getCanonicalState().getSlot().less(block.getSlot())) {
      result = perSlotTransition.apply(result);
      if (specHelpers.is_epoch_end(result.getCanonicalState().getSlot())) {
        result = perEpochTransition.apply(result);
      }
    }

    return perSlotTransition.apply(result);
  }

  @Override
  public Publisher<BeaconTuple> getBlockStatesStream() {
    return blockStream;
  }
}
