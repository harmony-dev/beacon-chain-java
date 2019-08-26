package org.ethereum.beacon.test.type.ssz.field;

import org.ethereum.beacon.test.type.DataMapperAccessor;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface ValueField extends DataMapperAccessor {
  default String getValue() {
    final String key = "value.yaml";
    if (!getFiles().containsKey(key)) {
      throw new RuntimeException("`value` not defined");
    }
    return new String(getFiles().get(key).extractArray(), UTF_8);
  }
}
