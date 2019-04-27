package org.ethereum.beacon.chain.storage.impl;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconTupleStorageImpl implements BeaconTupleStorage {

  private final BeaconBlockStorage blockStorage;
  private final BeaconStateStorage stateStorage;

  public BeaconTupleStorageImpl(BeaconBlockStorage blockStorage, BeaconStateStorage stateStorage) {
    this.blockStorage = blockStorage;
    this.stateStorage = stateStorage;
  }

  @Override
  public Optional<BeaconTuple> get(@Nonnull Hash32 hash) {
    Objects.requireNonNull(hash);
    return blockStorage
        .get(hash)
        .map(
            block ->
                stateStorage
                    .get(block.getStateRoot())
                    .map(
                        state ->
                            BeaconTuple.of(
                                block, new BeaconStateExImpl(state, TransitionType.UNKNOWN)))
                    .orElseThrow(
                        () -> new IllegalStateException("State inconsistency for block " + block)));
  }

  @Override
  public void put(@Nonnull Hash32 hash, @Nonnull BeaconTuple tuple) {
    put(tuple);
  }

  @Override
  public void remove(@Nonnull Hash32 hash) {
    Objects.requireNonNull(hash);
    blockStorage.remove(hash);
    stateStorage.remove(hash);
  }

  @Override
  public void flush() {
    blockStorage.flush();
    stateStorage.flush();
  }

  @Override
  public boolean isEmpty() {
    return blockStorage.isEmpty();
  }

  @Override
  public void put(@Nonnull BeaconTuple tuple) {
    Objects.requireNonNull(tuple);

    blockStorage.put(tuple.getBlock());
    stateStorage.put(tuple.getBlock().getStateRoot(), tuple.getState());
  }
}
