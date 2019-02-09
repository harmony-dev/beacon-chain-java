package org.ethereum.beacon.db.source.impl;

import javax.annotation.Nonnull;
import org.ethereum.beacon.db.source.CodecSource;
import org.ethereum.beacon.db.source.DataSource;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class PrefixedKeyDataSource<TValue>
    extends CodecSource.KeyOnly<BytesValue, TValue, BytesValue> {

  public PrefixedKeyDataSource(
      @Nonnull DataSource<BytesValue, TValue> upstreamSource,
      @Nonnull BytesValue prefix) {
    super(upstreamSource, prefix::concat);
  }
}
