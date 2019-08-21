package org.ethereum.beacon.test.type.state.field;

public interface Eth1TimestampField extends DataMapperAccessor {
  default Long getEth1Timestamp() {
    final String key = "eth1_timestamp.yaml";
    try {
      if (getFiles().containsKey(key)) {
        return getMapper().readValue(getFiles().get(key).extractArray(), Long.class);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new RuntimeException("`eth1_timestamp` not defined");
  }
}
