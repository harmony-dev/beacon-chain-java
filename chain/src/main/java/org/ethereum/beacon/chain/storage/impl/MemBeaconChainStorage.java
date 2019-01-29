package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.AbstractBeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.ethereum.beacon.db.source.impl.HashMapHoleyList;

public class MemBeaconChainStorage extends AbstractBeaconChainStorage {

  @Override
  public BeaconBlockStorage createBeaconBlockStorage() {
    return new BeaconBlockStorageImpl(
        new HashMapDataSource<>(), new HashMapHoleyList<>(), ChainSpec.DEFAULT);
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
