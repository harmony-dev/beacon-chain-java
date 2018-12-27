package org.ethereum.beacon.chain;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.validator.BeaconBlockValidator.Context;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.validator.BeaconBlockValidator;
import org.ethereum.beacon.consensus.validator.BeaconStateValidator;
import org.ethereum.beacon.consensus.validator.ValidationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.db.DBFlusher;

public class DefaultBeaconChain implements MutableBeaconChain {

  BeaconBlockStorage blockStorage;
  BeaconStateStorage stateStorage;
  BeaconTupleStorage tupleStorage;

  StateTransition<BeaconState> initialTransition;
  StateTransition<BeaconState> stateTransition;

  BeaconBlockValidator blockValidator;
  BeaconStateValidator stateValidator;

  DBFlusher dbFlusher;

  BeaconTuple head;

  @Override
  public void init() {
    Optional<BeaconTuple> head = tupleStorage.getCanonicalHead();
    if (!head.isPresent()) {
      initializeStorage();
      this.head = initializeStorage();
    } else {
      this.head = head.get();
    }
  }

  private BeaconTuple initializeStorage() {
    BeaconState initialState =
        initialTransition.applyBlock(BeaconBlocks.createGenesis(), BeaconState.createEmpty());
    BeaconBlock genesis = BeaconBlocks.createGenesis().withStateRoot(initialState.getHash());

    BeaconTuple tuple = BeaconTuple.create(genesis, initialState);
    tupleStorage.put(tuple);

    dbFlusher.flushSync();

    return tuple;
  }

  @Override
  public synchronized void insert(BeaconBlock block) {
    assert head != null;

    ValidationResult beaconValidation = blockValidator.validate(block, new Context());
    if (!beaconValidation.isPassed()) {
      return;
    }

    BeaconState preState = pullParentState(block);
    BeaconState postState = stateTransition.applyBlock(block, preState);

    ValidationResult stateValidation = stateValidator.validate(block, postState);
    if (!stateValidation.isPassed()) {
      return;
    }

    BeaconTuple newTuple = BeaconTuple.create(block, postState);

    tupleStorage.put(newTuple);
    dbFlusher.commit();

    this.head = newTuple;
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
