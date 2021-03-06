package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.test.type.DataMapperAccessor;

public interface IsValidField extends DataMapperAccessor {
  default Boolean isValid() {
    final String key = "is_valid.yaml";
    try {
      if (getFiles().containsKey(key)) {
        return getMapper().readValue(getFiles().get(key).extractArray(), Boolean.class);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new RuntimeException("`is_valid` not defined");
  }
}
