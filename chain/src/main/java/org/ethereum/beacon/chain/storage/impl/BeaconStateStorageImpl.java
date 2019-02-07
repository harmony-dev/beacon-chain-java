package org.ethereum.beacon.chain.storage.impl;

import java.util.function.Function;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.impl.DelegateDataSource;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class BeaconStateStorageImpl extends DelegateDataSource<Hash32, BeaconState> implements BeaconStateStorage {

  private final Function<Object, Hash32> hashFunction;

  public BeaconStateStorageImpl(
      Function<Object, Hash32> hashFunction,
      DataSource<Hash32, BeaconState> delegate) {
    super(delegate);
    this.hashFunction = hashFunction;
  }

  @Override
  public void put(BeaconState state) {
    this.put(hashFunction.apply(state), state);
  }
}
