package org.ethereum.beacon.db;

import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.util.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RocksDbDrivenDatabaseTest {

  @AfterEach
  @BeforeEach
  void cleanUp() throws IOException {
    FileUtil.removeRecursively("test-db");
  }

  @Test
  void basicOperations() {
    Database db = Database.rocksDB("test-db", -1);

    DataSource<BytesValue, BytesValue> uno = db.createStorage("uno");
    DataSource<BytesValue, BytesValue> dos = db.createStorage("dos");

    uno.put(wrap("ONE"), wrap("FIRST"));
    uno.put(wrap("TWO"), wrap("SECOND"));
    dos.put(wrap("TWO"), wrap("SECOND"));
    uno.put(wrap("THREE"), wrap("UNO_THIRD"));
    dos.put(wrap("FOUR"), wrap("DOS_FOURTH"));

    db.commit();
    assertThat(wrap("FIRST")).isEqualTo(uno.get(wrap("ONE")).get());
    assertThat(wrap("SECOND")).isEqualTo(uno.get(wrap("TWO")).get());
    assertThat(wrap("SECOND")).isEqualTo(dos.get(wrap("TWO")).get());
    assertThat(wrap("UNO_THIRD")).isEqualTo(uno.get(wrap("THREE")).get());
    assertThat(wrap("DOS_FOURTH")).isEqualTo(dos.get(wrap("FOUR")).get());
    assertThat(uno.get(wrap("FOUR"))).isNotPresent();
    assertThat(dos.get(wrap("ONE"))).isNotPresent();
    assertThat(dos.get(wrap("THREE"))).isNotPresent();
    assertThat(uno.get(wrap("FOUR"))).isNotPresent();

    uno.remove(wrap("TWO"));
    dos.put(wrap("THREE"), wrap("DOS_THIRD"));

    assertThat(uno.get(wrap("TWO"))).isNotPresent();
    assertThat(wrap("DOS_THIRD")).isEqualTo(dos.get(wrap("THREE")).get());
    assertThat(wrap("UNO_THIRD")).isEqualTo(uno.get(wrap("THREE")).get());

    db.commit();
    assertThat(uno.get(wrap("TWO"))).isNotPresent();
    assertThat(wrap("DOS_THIRD")).isEqualTo(dos.get(wrap("THREE")).get());
    assertThat(wrap("UNO_THIRD")).isEqualTo(uno.get(wrap("THREE")).get());

    dos.remove(wrap("FOUR"));
    uno.put(wrap("FOUR"), wrap("UNO_FOURTH"));
    assertThat(wrap("UNO_FOURTH")).isEqualTo(uno.get(wrap("FOUR")).get());
    assertThat(dos.get(wrap("FOUR"))).isNotPresent();

    db.commit();
    assertThat(wrap("UNO_FOURTH")).isEqualTo(uno.get(wrap("FOUR")).get());
    assertThat(dos.get(wrap("FOUR"))).isNotPresent();

    db.close();
  }

  @Test
  void reopenWithoutFlush() {
    Database db = Database.rocksDB("test-db", -1);

    DataSource<BytesValue, BytesValue> storage = db.createStorage("uno");

    storage.put(wrap("ONE"), wrap("FIRST"));
    storage.put(wrap("TWO"), wrap("SECOND"));
    storage.put(wrap("THREE"), wrap("THIRD"));

    db.close();

    storage = db.createStorage("uno");

    assertThat(wrap("FIRST")).isEqualTo(storage.get(wrap("ONE")).get());
    assertThat(wrap("SECOND")).isEqualTo(storage.get(wrap("TWO")).get());
    assertThat(wrap("THIRD")).isEqualTo(storage.get(wrap("THREE")).get());

    db.close();
  }

  private BytesValue wrap(String value) {
    return BytesValue.wrap(value.getBytes());
  }
}
