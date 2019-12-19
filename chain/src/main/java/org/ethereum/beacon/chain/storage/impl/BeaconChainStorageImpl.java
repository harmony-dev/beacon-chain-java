package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.envelops.SignedBeaconBlockHeader;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.SingleValueSource;
import tech.pegasys.artemis.ethereum.core.Hash32;

/** A default implementation of {@link BeaconChainStorage}. */
public class BeaconChainStorageImpl implements BeaconChainStorage {

  private final Database database;
  private final BeaconBlockStorage blockStorage;
  private final DataSource<Hash32, SignedBeaconBlockHeader> blockHeaderStorage;
  private final BeaconStateStorage stateStorage;
  private final BeaconTupleStorage tupleStorage;
  private final SingleValueSource<Checkpoint> justifiedStorage;
  private final SingleValueSource<Checkpoint> bestJustifiedStorage;
  private final SingleValueSource<Checkpoint> finalizedStorage;

  public BeaconChainStorageImpl(
      Database database,
      BeaconBlockStorage blockStorage,
      DataSource<Hash32, SignedBeaconBlockHeader> blockHeaderStorage,
      BeaconStateStorage stateStorage,
      BeaconTupleStorage tupleStorage,
      SingleValueSource<Checkpoint> justifiedStorage,
      SingleValueSource<Checkpoint> bestJustifiedStorage,
      SingleValueSource<Checkpoint> finalizedStorage) {
    this.database = database;
    this.blockStorage = blockStorage;
    this.blockHeaderStorage = blockHeaderStorage;
    this.stateStorage = stateStorage;
    this.tupleStorage = tupleStorage;
    this.justifiedStorage = justifiedStorage;
    this.bestJustifiedStorage = bestJustifiedStorage;
    this.finalizedStorage = finalizedStorage;
  }

  @Override
  public BeaconBlockStorage getBlockStorage() {
    return blockStorage;
  }

  @Override
  public DataSource<Hash32, SignedBeaconBlockHeader> getBlockHeaderStorage() {
    return blockHeaderStorage;
  }

  @Override
  public BeaconStateStorage getStateStorage() {
    return stateStorage;
  }

  @Override
  public BeaconTupleStorage getTupleStorage() {
    return tupleStorage;
  }

  @Override
  public SingleValueSource<Checkpoint> getJustifiedStorage() {
    return justifiedStorage;
  }

  @Override
  public SingleValueSource<Checkpoint> getBestJustifiedStorage() {
    return bestJustifiedStorage;
  }

  @Override
  public SingleValueSource<Checkpoint> getFinalizedStorage() {
    return finalizedStorage;
  }

  @Override
  public void commit() {
    tupleStorage.flush();
    database.commit();
  }
}
