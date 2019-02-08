package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.impl.DelegateDataSource;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconStateStorageImpl extends DelegateDataSource<Hash32, BeaconState>
    implements BeaconStateStorage {

  private final ObjectHasher<Hash32> objectHasher;

  public BeaconStateStorageImpl(
      ObjectHasher<Hash32> objectHasher, DataSource<Hash32, BeaconState> delegate) {
    super(delegate);
    this.objectHasher = objectHasher;
  }

  @Override
  public void put(BeaconState state) {
    this.put(objectHasher.getHash(state), state);
  }
}
