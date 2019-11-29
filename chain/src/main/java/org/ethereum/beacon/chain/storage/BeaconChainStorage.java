package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.SingleValueSource;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface BeaconChainStorage {

  BeaconBlockStorage getBlockStorage();

  DataSource<Hash32, BeaconBlockHeader> getBlockHeaderStorage();

  BeaconStateStorage getStateStorage();

  BeaconTupleStorage getTupleStorage();

  SingleValueSource<Checkpoint> getJustifiedStorage();

  SingleValueSource<Checkpoint> getBestJustifiedStorage();

  SingleValueSource<Checkpoint> getFinalizedStorage();

  void commit();
}
