package org.ethereum.beacon.test.type.state.tmp;

import java.util.Map;

public interface SlotsField extends FieldLoader {
  default Integer getPre() {
    try {
      for (Map.Entry<String, String> file : getFiles().entrySet()) {
        if (file.getKey().equals("slots.yaml")) {
          return getMapper().readValue(file.getValue(), Integer.class);
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new RuntimeException("`slots` field not defined");
  }
}
