package org.ethereum.beacon.test.type.ssz.field;

import org.ethereum.beacon.test.type.DataMapperAccessor;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface SerializedField extends DataMapperAccessor {
  default BytesValue getSerialized() {
    final String key = "serialized.ssz";
    if (!getFiles().containsKey(key)) {
      throw new RuntimeException("`serialized` not defined");
    }
    return getFiles().get(key);
  }
}
