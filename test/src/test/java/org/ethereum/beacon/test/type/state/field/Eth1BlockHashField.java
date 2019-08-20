package org.ethereum.beacon.test.type.state.field;

public interface Eth1BlockHashField extends DataMapperAccessor {
  default String getEth1BlockHash() {
    final String key = "eth1_block_hash.yaml";
    try {
      if (getFiles().containsKey(key)) {
        return getMapper().readValue(getFiles().get(key), String.class);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new RuntimeException("`eth1_block_hash` not defined");
  }
}
