package org.ethereum.beacon.start.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.db.InMemoryDatabase;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * DB Manager which incapsulates logic to create or remove/clean DB. This is probably an
 * intermediate solution. A better way may be extending {@link
 * org.ethereum.beacon.chain.storage.BeaconChainStorageFactory} with the appropriate functionality.
 */
public interface DatabaseManager {
  /** Creates a DB for a given &lt;genesisTime, depositRoot&gt; pair. */
  Database getOrCreateDatabase(Time genesisTime, Hash32 depositRoot);

  /** Removes/wipes a DB corresponding to a given &lt;genesisTime, depositRoot&gt; pair. */
  void removeDatabase(Time genesisTime, Hash32 depositRoot);

  static DatabaseManager createInMemoryDBFactory() {
    return new DatabaseManager() {
      @Override
      public Database getOrCreateDatabase(Time genesisTime, Hash32 depositRoot) {
        return new InMemoryDatabase();
      }

      @Override
      public void removeDatabase(Time genesisTime, Hash32 depositRoot) {}
    };
  }

  static DatabaseManager createRocksDBFactory(String dbPrefix, long bufferSize) {
    return new DatabaseManager() {
      @Override
      public Database getOrCreateDatabase(Time genesisTime, Hash32 depositRoot) {
        return Database.rocksDB(
            Paths.get(computeDbName(dbPrefix, genesisTime, depositRoot)).toString(), bufferSize);
      }

      @Override
      public void removeDatabase(Time genesisTime, Hash32 depositRoot) {
        Path path = Paths.get(computeDbName(dbPrefix, genesisTime, depositRoot));
        try {
          if (Files.exists(path)) {
            Files.list(path)
                .forEach(
                    f -> {
                      try {
                        Files.delete(f);
                      } catch (IOException e) {
                      }
                    });
            // Files.delete(path);
          }
        } catch (IOException e) {
          throw new RuntimeException("Cannot remove DB " + path.toString(), e);
        }
      }
    };
  }

  static String computeDbName(String dbPrefix, Time startTime, Hash32 depositRoot) {
    return String.format(
        "%s_start_time_%d_dep_root_%s",
        dbPrefix, startTime.getValue(), depositRoot.toStringShort());
  }
}
