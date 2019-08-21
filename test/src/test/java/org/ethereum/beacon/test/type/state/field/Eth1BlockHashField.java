package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.test.type.DataMapperAccessor;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface Eth1BlockHashField extends DataMapperAccessor {
  default String getEth1BlockHash() {
    final String key = useSszWhenPossible() ? "eth1_block_hash.ssz" : "eth1_block_hash.yaml";

    // SSZ
    if (useSszWhenPossible()) {
      return getSszSerializer().decode(getFiles().get(key), BytesValue.class).toString();
    }

    // YAML
    try {
      if (getFiles().containsKey(key)) {
        return getMapper().readValue(getFiles().get(key).extractArray(), String.class);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new RuntimeException("`eth1_block_hash` not defined");
  }
}
