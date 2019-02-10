package org.ethereum.beacon.db.ethj;

import java.util.function.Function;
import org.ethereum.beacon.db.XorKeyDatabase;
import org.ethereum.beacon.db.source.CodecSource;
import org.ethereum.datasource.Source;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class EthJDatabase extends XorKeyDatabase {

  public EthJDatabase(
      Source<byte[], byte[]> ethJDataSource, Function<BytesValue, BytesValue> sourceNameHasher) {
    super(
        new CodecSource<>(
            new EthJDataSourceAdapter<>(ethJDataSource),
            BytesValue::extractArray,
            BytesValue::extractArray,
            BytesValue::wrap),
        sourceNameHasher);
  }

  @Override
  public void commit() {
    // TODO
  }

  @Override
  public void close() {

  }
}
