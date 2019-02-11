package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.db.source.SingleValueSource;
import tech.pegasys.artemis.ethereum.core.Hash32;

/** A default implementation of {@link BeaconChainStorage}. */
public class BeaconChainStorageImpl implements BeaconChainStorage {

  private final BeaconBlockStorage blockStorage;
  private final BeaconStateStorage stateStorage;
  private final BeaconTupleStorage tupleStorage;
  private final SingleValueSource<Hash32> justifiedStorage;
  private final SingleValueSource<Hash32> finalizedStorage;

  public BeaconChainStorageImpl(
      BeaconBlockStorage blockStorage,
      BeaconStateStorage stateStorage,
      BeaconTupleStorage tupleStorage,
      SingleValueSource<Hash32> justifiedStorage,
      SingleValueSource<Hash32> finalizedStorage) {
    this.blockStorage = blockStorage;
    this.stateStorage = stateStorage;
    this.tupleStorage = tupleStorage;
    this.justifiedStorage = justifiedStorage;
    this.finalizedStorage = finalizedStorage;
  }

  @Override
  public BeaconBlockStorage getBlockStorage() {
    return blockStorage;
  }

  @Override
  public BeaconStateStorage getStateStorage() {
    return stateStorage;
  }

  @Override
  public BeaconTupleStorage getTupleStorage() {
    return tupleStorage;
  }

  @Override
  public SingleValueSource<Hash32> getJustifiedStorage() {
    return justifiedStorage;
  }

  @Override
  public SingleValueSource<Hash32> getFinalizedStorage() {
    return finalizedStorage;
  }

  @Override
  public void commit() {
    tupleStorage.flush();
  }
}
