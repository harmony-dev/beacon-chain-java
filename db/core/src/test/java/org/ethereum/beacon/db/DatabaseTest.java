package org.ethereum.beacon.db;

import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.util.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseTest {

  private static final String TEST_DB_NAME = "rocksdb";
  private static final BytesValue KEY = BytesValue.of(123);
  private static final BytesValue VALUE = BytesValue.of(1, 2, 3);

  @BeforeEach
  @AfterEach
  void setUp() throws IOException {
    FileUtil.removeRecursively(TEST_DB_NAME);
  }

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

  @Test
  void testCreateNotClosedDatabase() {
    final Database database = Database.rocksDB(TEST_DB_NAME, 0);
    database.createStorage("test-db");

    final Database newDatabase = Database.rocksDB(TEST_DB_NAME, 0);
    assertThatThrownBy(() -> newDatabase.createStorage("test-db"))
            .isInstanceOf(RuntimeException.class)
            .hasCauseExactlyInstanceOf(RocksDBException.class);

    database.close();
  }

  @ParameterizedTest
  @MethodSource("validArgumentsProvider")
  void testValidCreation(String path, long bufferLimitsInBytes) throws IOException {
    final Database database = Database.rocksDB(path, bufferLimitsInBytes);
    final DataSource<BytesValue, BytesValue> storage = database.createStorage("test-db");
    assertThat(Files.exists(Paths.get(path))).isTrue();

    storage.put(KEY, VALUE);
    assertThat(storage.get(KEY)).isPresent().hasValue(VALUE);
    storage.remove(KEY);
    assertThat(storage.get(KEY)).isNotPresent();

    database.close();
  }

  private static Stream<Arguments> validArgumentsProvider() {
    return Stream.of(
            Arguments.of(TEST_DB_NAME, 0),
            Arguments.of(TEST_DB_NAME, Long.MAX_VALUE),
            Arguments.of(TEST_DB_NAME, Long.MIN_VALUE));
  }

  @Test
  void testCommitAfterClose() {
    final Database database = Database.rocksDB(TEST_DB_NAME, Long.MAX_VALUE);
    final DataSource<BytesValue, BytesValue> storage = database.createStorage("test-db");

    storage.put(KEY, VALUE);
    database.close();

    //TODO: something should throw an Exception after close
    assertThatThrownBy(() -> storage.remove(KEY)).isInstanceOf(Exception.class);
    assertThatThrownBy(database::commit).isInstanceOf(Exception.class);
  }

  @Test
  void testCreateDuplicateStorage() {
    final Database database = Database.rocksDB(TEST_DB_NAME, Long.MAX_VALUE);
    final DataSource<BytesValue, BytesValue> s1 = database.createStorage("test-db");
    final DataSource<BytesValue, BytesValue> s2 = database.createStorage("test-db");

    s1.put(KEY, VALUE);
    assertThat(s1.get(KEY)).isPresent().hasValue(VALUE);
    assertThat(s1.get(KEY)).isEqualTo(s2.get(KEY));

    s2.put(KEY, BytesValue.of(3, 2, 1));
    assertThat(s2.get(KEY)).isPresent().hasValue(BytesValue.of(3, 2, 1));
    assertThat(s1.get(KEY)).isNotEqualTo(VALUE);
    assertThat(s1.get(KEY)).isEqualTo(s2.get(KEY));

    s1.remove(KEY);
    assertThat(s1.get(KEY)).isNotPresent();
    assertThat(s2.get(KEY)).isNotPresent();

    database.close();
  }

  @Test
  void testCreateDifferentStorage() {
    final Database database = Database.rocksDB(TEST_DB_NAME, Long.MAX_VALUE);
    final DataSource<BytesValue, BytesValue> s1 = database.createStorage("test-db");
    final DataSource<BytesValue, BytesValue> s2 = database.createStorage("test-db2");

    s1.put(KEY, VALUE);
    assertThat(s1.get(KEY)).isPresent().hasValue(VALUE);
    assertThat(s2.get(KEY)).isNotPresent();

    final BytesValue s2Value = BytesValue.of(3, 2, 1);
    s2.put(KEY, s2Value);
    assertThat(s2.get(KEY)).isPresent().hasValue(s2Value);
    assertThat(s1.get(KEY)).isNotEqualTo(s2Value);

    s1.remove(KEY);
    assertThat(s1.get(KEY)).isNotPresent();
    assertThat(s2.get(KEY)).isPresent().hasValue(s2Value);

    database.close();
  }

  @Test
  void testCreateDifferentDatabase_SameStorage() throws IOException {
    final Database db1 = Database.rocksDB(TEST_DB_NAME, Long.MAX_VALUE);
    final DataSource<BytesValue, BytesValue> s1 = db1.createStorage("test-db");

    final Database db2 = Database.rocksDB("rocksdb2", Long.MAX_VALUE);
    final DataSource<BytesValue, BytesValue> s2 = db2.createStorage("test-db2");

    s1.put(KEY, VALUE);
    assertThat(s1.get(KEY)).isPresent().hasValue(VALUE);
    assertThat(s2.get(KEY)).isNotPresent();

    final BytesValue s2Value = BytesValue.of(3, 2, 1);
    s2.put(KEY, s2Value);
    assertThat(s2.get(KEY)).isPresent().hasValue(s2Value);
    assertThat(s1.get(KEY)).isNotEqualTo(s2Value);

    s1.remove(KEY);
    assertThat(s1.get(KEY)).isNotPresent();
    assertThat(s2.get(KEY)).isPresent().hasValue(s2Value);

    db1.close();
    db2.close();
    FileUtil.removeRecursively("rocksdb2");
  }

  @Test
  void testSavedDataAfterReopen() {
    Database database = Database.rocksDB(TEST_DB_NAME, Long.MAX_VALUE);
    DataSource<BytesValue, BytesValue> storage = database.createStorage("test-db");
    storage.put(KEY, VALUE);
    database.commit();
    database.close();

    database = Database.rocksDB(TEST_DB_NAME, Long.MAX_VALUE);
    storage = database.createStorage("test-db");
    assertThat(storage.get(KEY)).isPresent().hasValue(VALUE);
    database.close();
  }

  @Test
  void testNotSaveUncommittedDataAfterReopen() {
    Database database = Database.rocksDB(TEST_DB_NAME, Long.MAX_VALUE);
    DataSource<BytesValue, BytesValue> storage = database.createStorage("test-db");
    storage.put(KEY, VALUE);
    database.close();

    database = Database.rocksDB(TEST_DB_NAME, Long.MAX_VALUE);
    storage = database.createStorage("test-db");
    assertThat(storage.get(KEY)).isNotPresent(); //TODO: data were not committed before
    database.close();
  }
}
