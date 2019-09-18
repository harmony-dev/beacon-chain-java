package org.ethereum.beacon.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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

    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private static Stream<Arguments> validArgumentsProvider() {
    return Stream.of(
            Arguments.of("/tmp/rocksdb", 0),
            Arguments.of("/tmp/rocksdb1", Long.MAX_VALUE),
            Arguments.of("/tmp/rocksdb2", Long.MIN_VALUE));
  }
}
