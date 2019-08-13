package org.ethereum.beacon.db;

import java.nio.file.Paths;
import org.ethereum.beacon.db.rocksdb.RocksDbSource;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.StorageEngineSource;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface Database {

  /**
   * Creates named key value storage if not yet exists or returns existing
   */
  DataSource<BytesValue, BytesValue> createStorage(String name);

  /**
   * Calling commit indicates that all current data is in consistent state
   * and it is a safe point to persist the data
   */
  void commit();

  /**
   * Close underlying database storage
   */
  void close();

  /**
   * Creates in-memory database instance.
   *
   * @return database instance.
   */
  static Database inMemoryDB() {
    return new InMemoryDatabase();
  }

  /**
   * Creates database instance driven by <a href="https://github.com/facebook/rocksdb">RocksDB</a>
   * storage engine.
   *
   * @param dbPath path to database folder.
   * @param bufferLimitInBytes limit of write buffer in bytes.
   * @return an instance of database driven by RocksDB.
   */
  static Database rocksDB(String dbPath, long bufferLimitInBytes) {
    StorageEngineSource<BytesValue> source = new RocksDbSource(Paths.get(dbPath));
    return EngineDrivenDatabase.create(source, bufferLimitInBytes);
  }
}
