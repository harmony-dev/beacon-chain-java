package org.ethereum.beacon.db;

import com.google.common.annotations.VisibleForTesting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.db.flush.BufferSizeObserver;
import org.ethereum.beacon.db.flush.DatabaseFlusher;
import org.ethereum.beacon.db.flush.InstantFlusher;
import org.ethereum.beacon.db.source.BatchWriter;
import org.ethereum.beacon.db.source.CacheSizeEvaluator;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.StorageEngineSource;
import org.ethereum.beacon.db.source.WriteBuffer;
import org.ethereum.beacon.db.source.impl.MemSizeEvaluators;
import org.ethereum.beacon.db.source.impl.XorDataSource;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class EngineDrivenDatabase implements Database {

  private static final Logger logger = LogManager.getLogger(EngineDrivenDatabase.class);

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
    WriteBuffer<BytesValue, BytesValue> buffer =
        new WriteBuffer<>(
            batchWriter,
            CacheSizeEvaluator.getInstance(MemSizeEvaluators.BytesValueEvaluator),
            true);
    DatabaseFlusher flusher =
        bufferLimitInBytes > 0
            ? BufferSizeObserver.create(buffer, bufferLimitInBytes)
            : new InstantFlusher(buffer);

    return new EngineDrivenDatabase(storageEngineSource, buffer, flusher);
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
    logger.info("Closing underlying database storage...");
    flusher.flush();
    source.close();
  }

  @VisibleForTesting
  WriteBuffer<BytesValue, BytesValue> getWriteBuffer() {
    return writeBuffer;
  }
}
