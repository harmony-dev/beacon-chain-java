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

  private final StorageEngineSource<BytesValue> storageEngineSource;
  private final WriteBuffer<BytesValue, BytesValue> writeBuffer;
  private final DatabaseFlusher flusher;

  EngineDrivenDatabase(
      StorageEngineSource<BytesValue> storageEngineSource,
      WriteBuffer<BytesValue, BytesValue> writeBuffer,
      DatabaseFlusher flusher) {
    this.storageEngineSource = storageEngineSource;
    this.writeBuffer = writeBuffer;
    this.flusher = flusher;
  }

  public static EngineDrivenDatabase createWithInstantFlusher(
      StorageEngineSource<BytesValue> storageEngineSource) {
    BatchWriter<BytesValue, BytesValue> batchWriter = new BatchWriter<>(storageEngineSource);
    WriteBuffer<BytesValue, BytesValue> writeBuffer =
        new WriteBuffer<>(
            batchWriter, CacheSizeEvaluator.getInstance(MemSizeEvaluators.BytesValueEvaluator));
    DatabaseFlusher flusher = DatabaseFlusher.instant(writeBuffer);

    return new EngineDrivenDatabase(storageEngineSource, writeBuffer, flusher);
  }

  public static EngineDrivenDatabase createWithBufferLimitFlusher(
      StorageEngineSource<BytesValue> storageEngineSource, long bufferLimitInBytes) {
    BatchWriter<BytesValue, BytesValue> batchWriter = new BatchWriter<>(storageEngineSource);
    WriteBuffer<BytesValue, BytesValue> writeBuffer =
        new WriteBuffer<>(
            batchWriter, CacheSizeEvaluator.getInstance(MemSizeEvaluators.BytesValueEvaluator));
    DatabaseFlusher flusher = DatabaseFlusher.limitedToSize(writeBuffer, bufferLimitInBytes);

    return new EngineDrivenDatabase(storageEngineSource, writeBuffer, flusher);
  }

  @Override
  public DataSource<BytesValue, BytesValue> createStorage(String name) {
    storageEngineSource.open();
    return new XorDataSource<>(writeBuffer, Hashes.sha256(BytesValue.wrap(name.getBytes())));
  }

  @Override
  public void commit() {
    flusher.commit();
  }

  @Override
  public void close() {
    storageEngineSource.close();
  }

  @VisibleForTesting
  WriteBuffer<BytesValue, BytesValue> getWriteBuffer() {
    return writeBuffer;
  }
}
