package org.ethereum.beacon.db.ethj;

import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.db.source.CodecSource;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.impl.XorDataSource;
import org.ethereum.datasource.Source;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.function.Function;

public class EthJDatabase implements Database {

  private final DataSource<BytesValue, BytesValue> backingDataSource;
  private final Function<BytesValue, BytesValue> sourceNameHasher;

  public EthJDatabase(Source<byte[], byte[]> ethJDataSource,
                      Function<BytesValue, BytesValue> sourceNameHasher) {
    this.sourceNameHasher = sourceNameHasher;
    EthJDataSourceAdapter<byte[], byte[]> adapter = new EthJDataSourceAdapter<>(ethJDataSource);
    backingDataSource = new CodecSource<>(adapter,
        BytesValue::extractArray,
        BytesValue::extractArray,
        BytesValue::wrap);
  }

  @Override
  public DataSource<BytesValue, BytesValue> createStorage(String name) {
    return new XorDataSource<>(backingDataSource,
        sourceNameHasher.apply(BytesValue.wrap(name.getBytes())));
  }

  @Override
  public void commit() {
    // TODO
  }

  @Override
  public void close() {

  }
}
