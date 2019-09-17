package org.ethereum.beacon.db.rocksdb;

import org.ethereum.beacon.db.util.FileUtil;
import org.junit.jupiter.api.*;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class RocksDbSourceTest {

  @AfterEach
  @BeforeEach
  void cleanUp() throws IOException {
    FileUtil.removeRecursively("test-db");
  }

  @Test
  void basicOperations() {
    RocksDbSource rocksDb = new RocksDbSource(Paths.get("test-db"));

    rocksDb.open();
    rocksDb.put(wrap("ONE"), wrap("FIRST"));

    assertThat(rocksDb.get(wrap("TWO"))).isNotPresent();
    assertThat(wrap("FIRST")).isEqualTo(rocksDb.get(wrap("ONE")).get());

    Map<BytesValue, BytesValue> batch = new HashMap<>();
    batch.put(wrap("ONE"), null);
    batch.put(wrap("TWO"), wrap("SECOND"));
    batch.put(wrap("THREE"), wrap("THIRD"));
    batch.put(wrap("FOUR"), wrap("FOURTH"));

    rocksDb.batchUpdate(batch);

    assertThat(rocksDb.get(wrap("ONE"))).isNotPresent();
    assertThat(wrap("SECOND")).isEqualTo(rocksDb.get(wrap("TWO")).get());
    assertThat(wrap("THIRD")).isEqualTo(rocksDb.get(wrap("THREE")).get());
    assertThat(wrap("FOURTH")).isEqualTo(rocksDb.get(wrap("FOUR")).get());

    rocksDb.remove(wrap("THREE"));
    assertThat(rocksDb.get(wrap("THREE"))).isNotPresent();

    rocksDb.close();
    rocksDb.open();

    assertThat(rocksDb.get(wrap("ONE"))).isNotPresent();
    assertThat(wrap("SECOND")).isEqualTo(rocksDb.get(wrap("TWO")).get());
    assertThat(rocksDb.get(wrap("THREE"))).isNotPresent();
    assertThat(wrap("FOURTH")).isEqualTo(rocksDb.get(wrap("FOUR")).get());
    assertThat(rocksDb.get(wrap("FIVE"))).isNotPresent();

    rocksDb.close();
  }

  private BytesValue wrap(String value) {
    return BytesValue.wrap(value.getBytes());
  }
}
