package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorageFactory;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.db.source.SingleValueSource;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.ethereum.beacon.db.source.impl.HashMapHoleyList;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class MemBeaconChainStorageFactory  implements BeaconChainStorageFactory {

  @Override
  public BeaconChainStorage create(Database database) {
    ObjectHasher<Hash32> objectHasher = ObjectHasher.createSSZOverKeccak256();

    BeaconBlockStorage blockStorage =
        new BeaconBlockStorageImpl(objectHasher, new HashMapDataSource<>(), new HashMapHoleyList<>());
    BeaconStateStorage stateStorage =
        new BeaconStateStorageImpl(new HashMapDataSource<>(), objectHasher);
    BeaconTupleStorage tupleStorage = new BeaconTupleStorageImpl(blockStorage, stateStorage);

    return new BeaconChainStorageImpl(blockStorage, stateStorage, tupleStorage,
        SingleValueSource.memSource(), SingleValueSource.memSource());
  }
}
