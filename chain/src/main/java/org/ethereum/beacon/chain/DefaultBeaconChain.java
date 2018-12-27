package org.ethereum.beacon.chain;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.db.DBFlusher;

public class DefaultBeaconChain implements MutableBeaconChain {

  BeaconBlockStorage blockStorage;
  BeaconStateStorage stateStorage;

  StateTransition<BeaconState> initialTransition;

  DBFlusher dbFlusher;

  BeaconBlock head;

  @Override
  public void init() {
    Optional<BeaconBlock> head = blockStorage.getCanonicalHead();
    if (!head.isPresent()) {
      initializeStorage();
      head = blockStorage.getCanonicalHead();
    }

    checkArgument(head.isPresent(), "Failed to get canonical head, storage is not yet initialized");
    this.head = head.get();
  }

  private void initializeStorage() {
    BeaconState initialState =
        initialTransition.applyBlock(BeaconBlocks.createGenesis(), BeaconState.createEmpty());
    BeaconBlock genesis = BeaconBlocks.createGenesis().withStateRoot(initialState.getHash());

    stateStorage.put(initialState.getHash(), initialState);
    blockStorage.put(genesis.getHash(), genesis);

    dbFlusher.flushSync();
  }

  @Override
  public void insert(BeaconBlock block) {
    assert head != null;
  }
}
