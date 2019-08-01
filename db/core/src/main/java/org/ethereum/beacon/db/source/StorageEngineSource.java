package org.ethereum.beacon.db.source;

import tech.pegasys.artemis.util.bytes.BytesValue;

public interface StorageEngineSource<ValueType>
    extends BatchUpdateDataSource<BytesValue, ValueType> {

  void open();

  void close();
}
