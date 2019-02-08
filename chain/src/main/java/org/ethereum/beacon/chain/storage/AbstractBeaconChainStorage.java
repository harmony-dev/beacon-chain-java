package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.chain.storage.impl.BeaconTupleStorageImpl;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import tech.pegasys.artemis.ethereum.core.Hash32;

public abstract class AbstractBeaconChainStorage implements BeaconChainStorage {

  private BeaconTupleStorage beaconTupleStorage;
  private BeaconBlockStorage beaconBlockStorage;
  private BeaconStateStorage beaconStateStorage;

  protected final ObjectHasher<Hash32> objectHasher;

  public AbstractBeaconChainStorage(ObjectHasher<Hash32> objectHasher) {
    this.objectHasher = objectHasher;
  }

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
    return new BeaconTupleStorageImpl(
        objectHasher, getBeaconBlockStorage(), getBeaconStateStorage());
  }
}
