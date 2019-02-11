package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.db.source.SingleValueSource;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface BeaconChainStorage {

  BeaconBlockStorage getBlockStorage();

  BeaconStateStorage getStateStorage();

  BeaconTupleStorage getTupleStorage();

  SingleValueSource<Hash32> getJustifiedStorage();

  SingleValueSource<Hash32> getFinalizedStorage();

  void commit();
}
