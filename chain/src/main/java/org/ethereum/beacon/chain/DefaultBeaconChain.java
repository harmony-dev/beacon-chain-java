package org.ethereum.beacon.chain;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.ScoreFunction;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier.Context;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.consensus.types.Score;

public class DefaultBeaconChain implements MutableBeaconChain {

  BeaconBlockStorage blockStorage;
  BeaconStateStorage stateStorage;
  BeaconTupleStorage tupleStorage;

  StateTransition<BeaconState> initialTransition;
  StateTransition<BeaconState> stateTransition;

  BeaconBlockVerifier blockValidator;
  BeaconStateVerifier stateValidator;

  ScoreFunction scoreFunction;

  Database database;

  BeaconChainHead head;

  @Override
  public void init() {
    Optional<BeaconTuple> aTuple = tupleStorage.getCanonicalHead();
    BeaconTuple headTuple = aTuple.orElseGet(this::initializeStorage);

    Score headScore = scoreFunction.apply(headTuple.getBlock(), headTuple.getState());
    this.head = BeaconChainHead.of(headTuple, headScore);
  }

  private BeaconTuple initializeStorage() {
    BeaconState initialState =
        initialTransition.apply(BeaconBlocks.createGenesis(), BeaconState.EMPTY);
    BeaconBlock genesis = BeaconBlocks.createGenesis().withStateRoot(initialState.getHash());

    BeaconTuple tuple = BeaconTuple.of(genesis, initialState);
    tupleStorage.put(tuple);

    database.flushSync();

    return tuple;
  }

  @Override
  public synchronized void insert(BeaconBlock block) {
    assert head != null;

    VerificationResult beaconValidation = blockValidator.validate(block, new Context());
    if (!beaconValidation.isPassed()) {
      return;
    }

    BeaconState parentState = pullParentState(block);
    BeaconState newState = stateTransition.apply(block, parentState);

    VerificationResult stateValidation = stateValidator.validate(block, newState);
    if (!stateValidation.isPassed()) {
      return;
    }

    BeaconTuple newTuple = BeaconTuple.of(block, newState);
    tupleStorage.put(newTuple);

    Score newScore = scoreFunction.apply(block, newState);
    if (head.getScore().compareTo(newScore) < 0) {
      blockStorage.reorgTo(newTuple.getBlock());
      this.head = BeaconChainHead.of(newTuple, newScore);
    }

    database.commit();
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
}
