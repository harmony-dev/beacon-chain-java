package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.core.BeaconBlock;
import tech.pegasys.artemis.ethereum.core.Hash32;

import javax.annotation.Nonnull;
import java.util.Optional;

public class BeaconBlockStorageImpl implements BeaconBlockStorage {

  @Override
  public Optional<BeaconBlock> getCanonicalHead() {
    return null;
  }

  @Override
  public void reorgTo(BeaconBlock block) {

  }

  @Override
  public Optional<BeaconBlock> get(@Nonnull Hash32 key) {
    return null;
  }

  @Override
  public void put(@Nonnull Hash32 key, @Nonnull BeaconBlock value) {

  }

  @Override
  public void remove(@Nonnull Hash32 key) {

  }

  @Override
  public void flush() {

  }
}
