package org.ethereum.beacon.db;

import com.google.common.annotations.VisibleForTesting;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.db.flush.DatabaseFlusher;
import org.ethereum.beacon.db.source.BatchWriter;
import org.ethereum.beacon.db.source.CacheSizeEvaluator;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.StorageEngineSource;
import org.ethereum.beacon.db.source.WriteBuffer;
import org.ethereum.beacon.db.source.impl.MemSizeEvaluators;
import org.ethereum.beacon.db.source.impl.XorDataSource;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class EngineDrivenDatabase implements Database {

  private final StorageEngineSource<BytesValue> source;
  private final WriteBuffer<BytesValue, BytesValue> writeBuffer;
  private final DatabaseFlusher flusher;

  EngineDrivenDatabase(
      StorageEngineSource<BytesValue> source,
      WriteBuffer<BytesValue, BytesValue> writeBuffer,
      DatabaseFlusher flusher) {
    this.source = source;
    this.writeBuffer = writeBuffer;
    this.flusher = flusher;
  }

  public static EngineDrivenDatabase create(
      StorageEngineSource<BytesValue> storageEngineSource, long bufferLimitInBytes) {
    BatchWriter<BytesValue, BytesValue> batchWriter = new BatchWriter<>(storageEngineSource);
    WriteBuffer<BytesValue, BytesValue> writeBuffer =
        new WriteBuffer<>(
            batchWriter, CacheSizeEvaluator.getInstance(MemSizeEvaluators.BytesValueEvaluator));
    DatabaseFlusher flusher = DatabaseFlusher.create(writeBuffer, bufferLimitInBytes);

    return new EngineDrivenDatabase(storageEngineSource, writeBuffer, flusher);
  }

  public static EngineDrivenDatabase createWithInstantFlusher(
      StorageEngineSource<BytesValue> storageEngineSource) {
    return create(storageEngineSource, -1);
  }

  @Override
  public DataSource<BytesValue, BytesValue> createStorage(String name) {
    source.open();
    return new XorDataSource<>(writeBuffer, Hashes.sha256(BytesValue.wrap(name.getBytes())));
  }

  @Override
  public void commit() {
    flusher.commit();
  }

  @Override
  public void close() {
    flusher.flush();
    source.close();
  }

  @VisibleForTesting
  WriteBuffer<BytesValue, BytesValue> getWriteBuffer() {
    return writeBuffer;
  }
}
