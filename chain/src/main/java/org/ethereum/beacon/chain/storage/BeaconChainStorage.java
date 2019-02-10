package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.chain.storage.impl.BeaconBlockStorageImpl;
import org.ethereum.beacon.chain.storage.impl.BeaconChainStorageImpl;
import org.ethereum.beacon.chain.storage.impl.BeaconStateStorageImpl;
import org.ethereum.beacon.chain.storage.impl.BeaconTupleStorageImpl;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.ssz.Serializer;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface BeaconChainStorage {

  BeaconBlockStorage getBeaconBlockStorage();

  BeaconStateStorage getBeaconStateStorage();

  BeaconTupleStorage getBeaconTupleStorage();

  void commit();

  static BeaconChainStorage create(
      Database database,
      ObjectHasher<Hash32> objectHasher,
      Serializer serializer,
      ChainSpec chainSpec) {
    BeaconBlockStorage blockStorage =
        BeaconBlockStorageImpl.create(database, objectHasher, serializer, chainSpec);
    BeaconStateStorage stateStorage =
        BeaconStateStorageImpl.create(database, objectHasher, serializer);
    BeaconTupleStorage tupleStorage = new BeaconTupleStorageImpl(blockStorage, stateStorage);

    return new BeaconChainStorageImpl(blockStorage, stateStorage, tupleStorage);
  }
}
