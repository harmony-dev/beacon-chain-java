package org.ethereum.beacon.db;

import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.util.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseTest {

  @Test
  void testInvalidCreation() {
    final Database database = Database.rocksDB("", 0);
    assertThatThrownBy(() -> database.createStorage("test-db"))
            .isInstanceOf(RuntimeException.class)
            .hasCauseExactlyInstanceOf(RocksDBException.class);
  }

  @Test
  void testInvalidCreationWithNull() {
    assertThatThrownBy(() -> Database.rocksDB(null, 0)).isInstanceOf(NullPointerException.class);
  }

  @ParameterizedTest
  @MethodSource("validArgumentsProvider")
  void testValidCreation(String path, long bufferLimitsInBytes) throws IOException {
    final Database database = Database.rocksDB(path, bufferLimitsInBytes);
    final DataSource<BytesValue, BytesValue> storage = database.createStorage("test-db");
    final Path directory = Paths.get(path);
    assertThat(Files.exists(directory)).isTrue();

    //TODO: how to test without casting
    storage.put(BytesValue.of(123), BytesValue.of(1, 2, 3));
    database.commit();
    storage.remove(BytesValue.of(123));
    database.commit();
    database.close();

    FileUtil.removeRecursively("rocksdb");
  }

  private static Stream<Arguments> validArgumentsProvider() {
    return Stream.of(
            Arguments.of("rocksdb", 0),
            Arguments.of("rocksdb2", Long.MAX_VALUE),
            Arguments.of("rocksdb3", Long.MIN_VALUE));
  }

  @Test
  void testCreateDifferentStoresForSameDb() throws IOException {
    final Database database = Database.rocksDB("rocksdb", Long.MAX_VALUE);
    final DataSource<BytesValue, BytesValue> s1 = database.createStorage("test-db");
    final DataSource<BytesValue, BytesValue> s2 = database.createStorage("test-db2");
    assertThat(Files.exists(Paths.get("rocksdb"))).isTrue();

    s1.put(BytesValue.of(123), BytesValue.of(1, 2, 3));
    s2.put(BytesValue.of(123), BytesValue.of(1, 2, 3));
    database.commit();
    database.close();
    s1.remove(BytesValue.of(123));
    s2.remove(BytesValue.of(123));
    assertThatThrownBy(database::commit).isInstanceOf(Exception.class); //TODO: should not commit after close

    FileUtil.removeRecursively("rocksdb");
  }

  @Test
  void testCreateDuplicateStorage() throws IOException {
    final Database database = Database.rocksDB("rocksdb", Long.MAX_VALUE);
    final DataSource<BytesValue, BytesValue> s1 = database.createStorage("test-db");
    final DataSource<BytesValue, BytesValue> s2 = database.createStorage("test-db");
    assertThat(Files.exists(Paths.get("rocksdb"))).isTrue();

    //TODO: how to test without casting
    s1.put(BytesValue.of(123), BytesValue.of(1, 2, 3));
    s2.put(BytesValue.of(123), BytesValue.of(1, 2, 3));
    database.commit();
    database.close();

    FileUtil.removeRecursively("rocksdb");
  }
}
