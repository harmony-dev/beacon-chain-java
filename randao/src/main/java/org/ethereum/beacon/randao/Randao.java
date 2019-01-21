package org.ethereum.beacon.randao;

import org.ethereum.beacon.db.source.DataSource;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** RANDAO interface. */
public interface Randao {

  static Randao get(DataSource<BytesValue, BytesValue> source) {
    return new Randao() {};
  }

  static Randao create(DataSource<BytesValue, BytesValue> source, int rounds) {
    return new Randao() {};
  }
}
