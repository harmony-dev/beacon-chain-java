package org.ethereum.beacon.db;

import org.ethereum.beacon.db.rocksdb.RocksDbSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseTest {

  @ParameterizedTest
  @MethodSource("invalidArgumentsProvider")
  void testInvalidCreation() {
    final EngineDrivenDatabase database = (EngineDrivenDatabase) Database.rocksDB("", 0);
    final RocksDbSource upstream = (RocksDbSource) database.getWriteBuffer().getUpstream();
    assertThatThrownBy(upstream::open)
            .isInstanceOf(RuntimeException.class)
            .hasCauseExactlyInstanceOf(RocksDBException.class);
  }

  private static Stream<Arguments> invalidArgumentsProvider() {
    return Stream.of(Arguments.of("", 0), Arguments.of("0123", 0));
  }

  @Test
  void testInvalidCreationWithNull() {
    assertThatThrownBy(() -> Database.rocksDB(null, 0)).isInstanceOf(NullPointerException.class);
  }

  @ParameterizedTest
  @MethodSource("validArgumentsProvider")
  void testValidCreation(String path, long bufferLimitsInBytes) {
    final EngineDrivenDatabase database = (EngineDrivenDatabase) Database.rocksDB(path, bufferLimitsInBytes);
    final RocksDbSource upstream = (RocksDbSource) database.getWriteBuffer().getUpstream();
    upstream.open();
    upstream.put(BytesValue.of(123), BytesValue.of(1, 2, 3));
    assertThat(database.getWriteBuffer().get(BytesValue.of(123))).isPresent().hasValue(BytesValue.of(1, 2, 3));
    upstream.close();
  }

  private static Stream<Arguments> validArgumentsProvider() {
    return Stream.of(
            Arguments.of("", 0),
            Arguments.of("/tmp", 0),
            Arguments.of("/tmp", Long.MAX_VALUE),
            Arguments.of("/tmp", Long.MIN_VALUE));
  }
}
