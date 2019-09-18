package org.ethereum.beacon.db;

import org.ethereum.beacon.db.source.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

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
    storage.put(BytesValue.of(123), BytesValue.of(1, 2, 3));
    database.commit();

    Files.deleteIfExists(Paths.get(path));
  }

  private static Stream<Arguments> validArgumentsProvider() {
    return Stream.of(
            Arguments.of("/tmp/rocksdb", 0),
            Arguments.of("/tmp/rocksdb", 1),
            Arguments.of("/tmp/rocksdb", -1));
  }
}
