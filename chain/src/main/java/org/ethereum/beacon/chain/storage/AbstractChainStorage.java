package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.chain.storage.ChainStorage;
import org.ethereum.beacon.chain.storage.impl.BeaconTupleStorageImpl;
import org.ethereum.beacon.db.Database;

public abstract class AbstractChainStorage implements ChainStorage {

  private BeaconTupleStorage beaconTupleStorage;
  private BeaconBlockStorage beaconBlockStorage;
  private BeaconStateStorage beaconStateStorage;

  @Override
  public BeaconBlockStorage getBeaconBlockStorage() {
    if (beaconBlockStorage == null) {
      beaconBlockStorage = createBeaconBlockStorage();
    }
    return beaconBlockStorage;
  }

  public abstract BeaconBlockStorage createBeaconBlockStorage();

    @Override
  public BeaconStateStorage getBeaconStateStorage() {
    if (beaconStateStorage == null) {
      beaconStateStorage = createBeaconStateStorage();
    }
    return beaconStateStorage;
  }

  protected abstract BeaconStateStorage createBeaconStateStorage();

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
}
