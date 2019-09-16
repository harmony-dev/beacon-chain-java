package org.ethereum.beacon.db;

import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class XorKeyDatabaseTest {

  private XorKeyDatabase db;

  @BeforeEach
  void setUp() {
    final HashMapDataSource<BytesValue, BytesValue> mapds = new HashMapDataSource<>();
    db = new XorKeyDatabase(mapds, Function.identity()) {
      @Override
      public void commit() {
      }

      @Override
      public void close() {
      }
    };
  }

  @Test
  void testCreateStorage_EmptyStorage() {
    final DataSource<BytesValue, BytesValue> storage1 = db.createStorage("Storage1");
    final BytesValue key = BytesValue.of(1, 2, 3);
    final BytesValue value = BytesValue.of(1, 4);
    storage1.put(key, value);
    assertThat(storage1.get(key)).isPresent().hasValue(value);
  }

  @Test
  void testCreateStorage() {
    final DataSource<BytesValue, BytesValue> storage0 = db.createStorage("Storage1");
    final BytesValue key = BytesValue.of(1, 2, 3);
    final BytesValue value = BytesValue.of(1, 4);
    storage0.put(key, value);
    final DataSource<BytesValue, BytesValue> storage1 = db.createStorage("Storage1");
    assertThat(storage1.get(key)).isPresent().hasValue(value);
  }

  @Test
  void testCreateStorage_1() {
    final DataSource<BytesValue, BytesValue> storage0 = db.createStorage("Storage1");
    final BytesValue key = BytesValue.of(1, 2, 3);
    final BytesValue storage0_value = BytesValue.of(1, 4);
    storage0.put(key, storage0_value);

    final DataSource<BytesValue, BytesValue> storage1 = db.createStorage("Storage2");
    assertThat(storage1.get(key)).isNotPresent();

    final BytesValue storage1_value = BytesValue.of(1, 4, 5);
    storage1.put(key, storage1_value);
    assertThat(storage1.get(key)).isPresent().hasValue(storage1_value);
    assertThat(storage0.get(key)).isPresent().hasValue(storage0_value);
  }
}
