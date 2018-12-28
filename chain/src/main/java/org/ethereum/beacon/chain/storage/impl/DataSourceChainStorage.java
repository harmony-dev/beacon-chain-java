package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.chain.storage.ChainStorage;
import org.ethereum.beacon.db.Database;

/**
 * Created by Anton Nashatyrev on 27.12.2018.
 */
public class DataSourceChainStorage implements ChainStorage {

  private final Database database;
  private BeaconTupleStorage beaconTupleStorage;
  private BeaconBlockStorage beaconBlockStorage;
  private BeaconStateStorage beaconStateStorage;

  public DataSourceChainStorage(Database database) {
    this.database = database;
  }

  @Override
  public BeaconBlockStorage getBeaconBlockStorage() {
    // TODO serializers needed
    return null;
  }

  @Override
  public BeaconStateStorage getBeaconStateStorage() {
    // TODO serializers needed
    return null;
  }

  @Override
  public BeaconTupleStorage getBeaconTupleStorage() {
    if (beaconTupleStorage == null) {
      beaconTupleStorage = createBeaconTupleStorage();
    }
    return beaconTupleStorage;
  }

  protected BeaconTupleStorage createBeaconTupleStorage() {
    return new BeaconTupleStorageImpl(getBeaconBlockStorage(), getBeaconStateStorage());
  }

  @Override
  public void commit() {
    database.commit();
  }
}
