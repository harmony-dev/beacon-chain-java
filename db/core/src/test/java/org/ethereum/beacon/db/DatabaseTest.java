package org.ethereum.beacon.db;

import org.ethereum.beacon.db.util.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;

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
    database.createStorage("test-db");
    final Path directory = Paths.get(path);
    assertThat(Files.exists(directory)).isTrue();

    FileUtil.removeRecursively("rocksdb");
  }

  private static Stream<Arguments> validArgumentsProvider() {
    return Stream.of(
            Arguments.of("rocksdb", 0),
            Arguments.of("rocksdb2", Long.MAX_VALUE),
            Arguments.of("rocksdb3", Long.MIN_VALUE));
  }
}
