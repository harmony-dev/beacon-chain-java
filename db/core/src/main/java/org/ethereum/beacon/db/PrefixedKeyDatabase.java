package org.ethereum.beacon.db;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.impl.PrefixedKeyDataSource;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;

/**
 * Uses {@link PrefixedKeyDatabase} to multiplex different storage keys.
 *
 * <p>The most significant byte of {@code name.hashCode()} product is used as a prefix.
 */
public abstract class PrefixedKeyDatabase implements Database {

  private final DataSource<BytesValue, BytesValue> backingDataSource;

  public PrefixedKeyDatabase(DataSource<BytesValue, BytesValue> backingDataSource) {
    this.backingDataSource = backingDataSource;
  }

  @Override
  public DataSource<BytesValue, BytesValue> createStorage(@Nonnull String name) {
    Objects.requireNonNull(name);
    return new PrefixedKeyDataSource<>(
        backingDataSource, BytesValues.ofUnsignedByte(name.hashCode() >>> 24));
  }
}
