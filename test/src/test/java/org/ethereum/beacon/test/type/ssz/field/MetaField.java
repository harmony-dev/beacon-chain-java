package org.ethereum.beacon.test.type.ssz.field;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.test.type.DataMapperAccessor;

public interface MetaField extends DataMapperAccessor {
  default String getRoot() {
    final String key = "meta.yaml";
    try {
      return getMapper().readValue(getFiles().get(key).extractArray(), RootsClass.class).getRoot();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  class RootsClass {
    private String root;

    public String getRoot() {
      return root;
    }

    public void setRoot(String root) {
      this.root = root;
    }
  }
}
