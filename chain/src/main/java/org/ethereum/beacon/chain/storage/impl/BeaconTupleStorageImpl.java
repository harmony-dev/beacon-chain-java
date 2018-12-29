package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import tech.pegasys.artemis.ethereum.core.Hash32;

import javax.annotation.Nonnull;
import java.util.Optional;

public class BeaconTupleStorageImpl implements BeaconTupleStorage {

  private final BeaconBlockStorage blockStorage;
  private final BeaconStateStorage stateStorage;

  public BeaconTupleStorageImpl(BeaconBlockStorage blockStorage,
                                BeaconStateStorage stateStorage) {
    this.blockStorage = blockStorage;
    this.stateStorage = stateStorage;
  }

  @Override
  public Optional<BeaconTuple> get(Hash32 hash) {
    return blockStorage.get(hash).map(block ->
        stateStorage.get(hash)
            .map(state -> BeaconTuple.of(block, state))
            .orElseThrow(() -> new IllegalStateException("State inconsistency for block "+ block))
    );
  }

  @Override
  public void put(Hash32 hash, BeaconTuple tuple) {
    assert hash.equals(tuple.getHash());

    blockStorage.put(tuple.getBlock());
    stateStorage.put(tuple.getState());
  }

  @Override
  public void remove(@Nonnull Hash32 key) {
    blockStorage.remove(key);
    stateStorage.remove(key);
  }

  @Override
  public void flush() {
    blockStorage.flush();
    stateStorage.flush();
  }

  @Override
  public Optional<BeaconTuple> getCanonicalHead() {
    return blockStorage.getCanonicalHead().flatMap(this::get);
  }
}
