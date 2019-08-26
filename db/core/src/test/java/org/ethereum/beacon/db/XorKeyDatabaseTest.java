package org.ethereum.beacon.db;

import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.*;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class XorKeyDatabaseTest {

  private XorKeyDatabase db;

  @BeforeEach
  public void setUp() {
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
  public void testCreateStorage_EmptyStorage() {
    final DataSource<BytesValue, BytesValue> storage1 = db.createStorage("Storage1");
    storage1.put(BytesValue.of(1, 2, 3), BytesValue.of(1, 4));
    assertThat(storage1.get(BytesValue.of(1, 2, 3))).isPresent();
  }

  @Test
  public void testCreateStorage() {
    final DataSource<BytesValue, BytesValue> storage0 = db.createStorage("Storage1");
    storage0.put(BytesValue.of(1, 2, 3), BytesValue.of(1, 4));
    final DataSource<BytesValue, BytesValue> storage1 = db.createStorage("Storage1");
    assertThat(storage1.get(BytesValue.of(1, 2, 3))).isPresent();
  }

  @Test
  public void testCreateStorage_1() {
    final DataSource<BytesValue, BytesValue> storage0 = db.createStorage("Storage1");
    storage0.put(BytesValue.of(1, 2, 3), BytesValue.of(1, 4));
    final DataSource<BytesValue, BytesValue> storage1 = db.createStorage("Storage2");
    assertThat(storage1.get(BytesValue.of(1, 2, 3))).isNotPresent();
    storage1.put(BytesValue.of(1, 2, 3), BytesValue.of(1, 4, 5));
    assertThat(storage1.get(BytesValue.of(1, 2, 3))).isPresent();
    assertThat(storage1.get(BytesValue.of(1, 2, 3)).get()).isEqualTo(BytesValue.of(1, 4, 5));

    assertThat(storage0.get(BytesValue.of(1, 2, 3))).isPresent();
    assertThat(storage0.get(BytesValue.of(1, 2, 3)).get()).isEqualTo(BytesValue.of(1, 4));
  }
}
