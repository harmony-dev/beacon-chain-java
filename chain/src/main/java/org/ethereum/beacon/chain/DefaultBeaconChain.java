package org.ethereum.beacon.chain;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.ScoreFunction;
import org.ethereum.beacon.consensus.validator.BeaconBlockValidator.Context;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.validator.BeaconBlockValidator;
import org.ethereum.beacon.consensus.validator.BeaconStateValidator;
import org.ethereum.beacon.consensus.validator.ValidationResult;
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

  BeaconBlockValidator blockValidator;
  BeaconStateValidator stateValidator;

  ScoreFunction scoreFunction;

  Database database;

  BeaconChainHead head;

  @Override
  public void init() {
    if (blockStorage.isEmpty()) {
      initializeStorage();
    }
    BeaconTuple headTuple = tupleStorage.getCanonicalHead();

    Score headScore = scoreFunction.apply(headTuple.getBlock(), headTuple.getState());
    this.head = BeaconChainHead.of(headTuple, headScore);
  }

  private BeaconTuple initializeStorage() {
    BeaconState initialState =
        initialTransition.apply(BeaconBlocks.createGenesis(), BeaconState.EMPTY);
    BeaconBlock genesis = BeaconBlocks.createGenesis().withStateRoot(initialState.getHash());

    BeaconTuple tuple = BeaconTuple.of(genesis, initialState);
    tupleStorage.put(tuple);

    return tuple;
  }

  @Override
  public synchronized void insert(BeaconBlock block) {
    assert head != null;

    ValidationResult beaconValidation = blockValidator.validate(block, new Context());
    if (!beaconValidation.isPassed()) {
      return;
    }

    BeaconState parentState = pullParentState(block);
    BeaconState newState = stateTransition.apply(block, parentState);

    ValidationResult stateValidation = stateValidator.validate(block, newState);
    if (!stateValidation.isPassed()) {
      return;
    }

    BeaconTuple newTuple = BeaconTuple.of(block, newState);
    tupleStorage.put(newTuple);

    Score newScore = scoreFunction.apply(block, newState);
    if (head.getScore().compareTo(newScore) < 0) {
      blockStorage.reorgTo(newTuple.getBlock().getHash());
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
