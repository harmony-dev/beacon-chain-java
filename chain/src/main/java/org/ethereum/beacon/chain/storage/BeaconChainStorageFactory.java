package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.chain.storage.impl.SSZBeaconChainStorageFactory;
import org.ethereum.beacon.db.Database;

/** A factory for {@link BeaconChainStorage}. */
public interface BeaconChainStorageFactory {

  BeaconChainStorage create(Database database);

  static BeaconChainStorageFactory get() {
    return new SSZBeaconChainStorageFactory();
  }
}
