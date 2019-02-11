package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorageFactory;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.SingleValueSource;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * An implementation which passes {@link SSZObjectHasher} and {@link SSZSerializerFactory} to {@link
 * BeaconChainStorage} instance.
 */
public class SSZBeaconChainStorageFactory implements BeaconChainStorageFactory {

  @Override
  public BeaconChainStorage create(Database database) {
    ObjectHasher<Hash32> objectHasher = ObjectHasher.createSSZOverKeccak256();
    SerializerFactory serializerFactory = SerializerFactory.createSSZ();

    BeaconBlockStorage blockStorage =
        BeaconBlockStorageImpl.create(database, objectHasher, serializerFactory);
    BeaconStateStorage stateStorage =
        BeaconStateStorageImpl.create(database, objectHasher, serializerFactory);
    BeaconTupleStorage tupleStorage = new BeaconTupleStorageImpl(blockStorage, stateStorage);

    SingleValueSource<Hash32> justifiedStorage = createHash32Storage(database, "justified-hash");
    SingleValueSource<Hash32> finalizedStorage = createHash32Storage(database, "finalized-hash");

    return new BeaconChainStorageImpl(
        blockStorage, stateStorage, tupleStorage, justifiedStorage, finalizedStorage);
  }

  private SingleValueSource<Hash32> createHash32Storage(Database database, String name) {
    DataSource<BytesValue, BytesValue> justifiedStorageSource = database.createStorage(name);
    return SingleValueSource.fromDataSource(
        justifiedStorageSource,
        BytesValue.wrap(name.getBytes()),
        hash -> hash,
        bytes -> Hash32.wrap(Bytes32.wrap(bytes, 0)));
  }
}
