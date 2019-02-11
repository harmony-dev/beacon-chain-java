package org.ethereum.beacon.db;

import java.util.function.Function;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.impl.XorDataSource;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** An abstract class that uses {@link XorDataSource} for storage multiplexing. */
public abstract class XorKeyDatabase implements Database {

  private final DataSource<BytesValue, BytesValue> backingDataSource;
  private final Function<BytesValue, BytesValue> sourceNameHasher;

  public XorKeyDatabase(
      DataSource<BytesValue, BytesValue> backingDataSource,
      Function<BytesValue, BytesValue> sourceNameHasher) {
    this.backingDataSource = backingDataSource;
    this.sourceNameHasher = sourceNameHasher;
  }

  @Override
  public DataSource<BytesValue, BytesValue> createStorage(String name) {
    return new XorDataSource<>(
        backingDataSource, sourceNameHasher.apply(BytesValue.wrap(name.getBytes())));
  }

  public DataSource<BytesValue, BytesValue> getBackingDataSource() {
    return backingDataSource;
  }
}
