package org.ethereum.beacon.chain.storage.impl;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.ethereum.beacon.chain.storage.AbstractHashKeyStorage;
import org.ethereum.beacon.chain.storage.BeaconStateStorage;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.db.source.CodecSource;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.ssz.Serializer;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class BeaconStateStorageImpl extends AbstractHashKeyStorage<Hash32, BeaconState>
    implements BeaconStateStorage {

  private final DataSource<Hash32, BeaconState> source;

  public BeaconStateStorageImpl(
      DataSource<Hash32, BeaconState> source, ObjectHasher<Hash32> objectHasher) {
    super(objectHasher);
    this.source = source;
  }

  @Override
  public Optional<BeaconState> get(@Nonnull Hash32 key) {
    Objects.requireNonNull(key);
    return source.get(key);
  }

  @Override
  public void put(@Nonnull Hash32 key, @Nonnull BeaconState value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    source.put(key, value);
  }

  @Override
  public void remove(@Nonnull Hash32 key) {
    Objects.requireNonNull(key);
    source.remove(key);
  }

  @Override
  public void flush() {
    // nothing to be done here. No cached data in this implementation
  }

  public static BeaconStateStorageImpl create(
      Database database, ObjectHasher<Hash32> objectHasher, Serializer serializer) {
    DataSource<BytesValue, BytesValue> backingSource = database.createStorage("beacon-state");
    DataSource<Hash32, BeaconState> stateSource =
        new CodecSource<>(
            backingSource,
            key -> key,
            serializer::encode2,
            bytes -> serializer.decode(bytes, BeaconStateImpl.class));
    return new BeaconStateStorageImpl(stateSource, objectHasher);
  }
}
