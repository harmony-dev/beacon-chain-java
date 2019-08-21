package org.ethereum.beacon.test.type.state.field;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.test.type.DataMapperAccessor;

public interface BlsSettingField extends DataMapperAccessor {
  default Integer getBlsSetting() {
    final String key = "meta.yaml";
    try {
      if (getFiles().containsKey(key)) {
        return getMapper().readValue(getFiles().get(key).extractArray(), MetaClass.class).getBlsSetting();
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    return null;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  class MetaClass {
    @JsonProperty("bls_setting")
    private Integer blsSetting;

    Integer getBlsSetting() {
      return blsSetting;
    }

    public void setBlsSetting(Integer blsSetting) {
      this.blsSetting = blsSetting;
    }
  }
}
