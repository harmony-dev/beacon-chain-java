package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;

/** A default implementation of {@link BeaconChainStorage}. */
public class BeaconChainStorageImpl implements BeaconChainStorage {

  private final BeaconBlockStorage blockStorage;
  private final BeaconStateStorage stateStorage;
  private final BeaconTupleStorage tupleStorage;

  public BeaconChainStorageImpl(
      BeaconBlockStorage blockStorage,
      BeaconStateStorage stateStorage,
      BeaconTupleStorage tupleStorage) {
    this.blockStorage = blockStorage;
    this.stateStorage = stateStorage;
    this.tupleStorage = tupleStorage;
  }

  @Override
  public BeaconBlockStorage getBeaconBlockStorage() {
    return blockStorage;
  }

  @Override
  public BeaconStateStorage getBeaconStateStorage() {
    return stateStorage;
  }

  @Override
  public BeaconTupleStorage getBeaconTupleStorage() {
    return tupleStorage;
  }

  @Override
  public void commit() {
    tupleStorage.flush();
  }
}
