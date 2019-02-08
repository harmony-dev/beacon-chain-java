package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.AbstractBeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.ethereum.beacon.db.source.impl.HashMapHoleyList;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class MemBeaconChainStorage extends AbstractBeaconChainStorage {

  public MemBeaconChainStorage(ObjectHasher<Hash32> objectHasher) {
    super(objectHasher);
  }

  @Override
  public BeaconBlockStorage createBeaconBlockStorage() {
    return new BeaconBlockStorageImpl(
        objectHasher, new HashMapDataSource<>(), new HashMapHoleyList<>(), ChainSpec.DEFAULT);
  }

  @Override
  protected BeaconStateStorage createBeaconStateStorage() {
    return new BeaconStateStorageImpl(objectHasher, new HashMapDataSource<>());
  }

  @Override
  public void commit() {
    // nothing to do
  }
}
