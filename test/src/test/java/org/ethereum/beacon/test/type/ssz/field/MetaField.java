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

  default String getSigningRoot() {
    final String key = "meta.yaml";
    try {
      if (getFiles().containsKey(key)) {
        return getMapper()
            .readValue(getFiles().get(key).extractArray(), RootsClass.class)
            .getSigningRoot();
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    return null; // XXX: optional field
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  class RootsClass {
    private String root;

    @JsonProperty("signing_root")
    private String signingRoot;

    public String getRoot() {
      return root;
    }

    public void setRoot(String root) {
      this.root = root;
    }

    public String getSigningRoot() {
      return signingRoot;
    }

    public void setSigningRoot(String signingRoot) {
      this.signingRoot = signingRoot;
    }
  }
}
