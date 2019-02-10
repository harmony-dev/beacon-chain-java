package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.chain.storage.impl.BeaconBlockStorageImpl;
import org.ethereum.beacon.chain.storage.impl.BeaconChainStorageImpl;
import org.ethereum.beacon.chain.storage.impl.BeaconStateStorageImpl;
import org.ethereum.beacon.chain.storage.impl.BeaconTupleStorageImpl;
import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.db.Database;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface BeaconChainStorage {

  BeaconBlockStorage getBeaconBlockStorage();

  BeaconStateStorage getBeaconStateStorage();

  BeaconTupleStorage getBeaconTupleStorage();

  void commit();

  static BeaconChainStorage createWithSSZ(
      Database database,
      ChainSpec chainSpec) {
    ObjectHasher<Hash32> objectHasher = ObjectHasher.createSSZOverKeccak256();
    SerializerFactory serializerFactory = SerializerFactory.createSSZ();

    BeaconBlockStorage blockStorage =
        BeaconBlockStorageImpl.create(database, objectHasher, serializerFactory, chainSpec);
    BeaconStateStorage stateStorage =
        BeaconStateStorageImpl.create(database, objectHasher, serializerFactory);
    BeaconTupleStorage tupleStorage = new BeaconTupleStorageImpl(blockStorage, stateStorage);

    return new BeaconChainStorageImpl(blockStorage, stateStorage, tupleStorage);
  }
}
