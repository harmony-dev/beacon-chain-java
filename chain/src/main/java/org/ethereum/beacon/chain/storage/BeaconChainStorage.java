package org.ethereum.beacon.chain.storage;

public interface BeaconChainStorage {

  BeaconBlockStorage getBeaconBlockStorage();

  BeaconStateStorage getBeaconStateStorage();

  BeaconTupleStorage getBeaconTupleStorage();

  void commit();
}
