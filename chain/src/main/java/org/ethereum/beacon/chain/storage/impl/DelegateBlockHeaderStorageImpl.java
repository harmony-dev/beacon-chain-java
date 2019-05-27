package org.ethereum.beacon.chain.storage.impl;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.db.source.DataSource;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class DelegateBlockHeaderStorageImpl implements DataSource<Hash32, BeaconBlockHeader> {

  private final BeaconBlockStorage delegateBlockStorage;
  private final ObjectHasher<Hash32> objectHasher;

  public DelegateBlockHeaderStorageImpl(
      BeaconBlockStorage delegateBlockStorage,
      ObjectHasher<Hash32> objectHasher) {
    this.delegateBlockStorage = delegateBlockStorage;
    this.objectHasher = objectHasher;
  }

  @Override
  public Optional<BeaconBlockHeader> get(@Nonnull Hash32 key) {
    return delegateBlockStorage
        .get(key)
        .map(this::createHeader);
  }

  private BeaconBlockHeader createHeader(BeaconBlock block) {
    return new BeaconBlockHeader(
        block.getSlot(),
        block.getPreviousBlockRoot(),
        block.getStateRoot(),
        objectHasher.getHash(block.getBody()),
        block.getSignature());
  }

  @Override
  public void put(@Nonnull Hash32 key, @Nonnull BeaconBlockHeader value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(@Nonnull Hash32 key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void flush() {
    throw new UnsupportedOperationException();
  }
}
