package org.ethereum.beacon.test.type.state.field;

public interface SlotsField extends DataMapperAccessor {
  default Integer getSlots() {
    final String key = "slots.yaml";
    try {
      if (getFiles().containsKey(key)) {
        return getMapper().readValue(getFiles().get(key).extractArray(), Integer.class);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new RuntimeException("`slots` not defined");
  }
}
