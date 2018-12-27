package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.db.Database;

/**
 * Created by Anton Nashatyrev on 27.12.2018.
 */
public interface ChainStorage extends Database {

  BeaconBlockStorage getBeaconBlockStorage();

  BeaconStateStorage getBeaconStateStorage();

  BeaconTupleStorage getBeaconTupleStorage();
}
