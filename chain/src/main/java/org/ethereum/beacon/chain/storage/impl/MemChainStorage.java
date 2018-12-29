package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.AbstractChainStorage;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.ethereum.beacon.db.source.impl.HashMapHoleyList;

public class MemChainStorage extends AbstractChainStorage {

  @Override
  public BeaconBlockStorage createBeaconBlockStorage() {
    return new BeaconBlockStorageImpl(new HashMapDataSource<>(), new HashMapHoleyList<>());
  }

  @Override
  protected BeaconStateStorage createBeaconStateStorage() {
    return new BeaconStateStorageImpl(new HashMapDataSource<>());
  }

  @Override
  public void commit() {
    // nothing to do
  }
}
