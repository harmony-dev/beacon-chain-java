package org.ethereum.beacon.test.type.state.tmp;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public interface BlsSettingField extends FieldLoader {
  default Integer getBlsSetting() {
    try {
      for (Map.Entry<String, String> file : getFiles().entrySet()) {
        if (file.getKey().equals("meta.yaml")) {
          return getMapper().readValue(file.getValue(), MetaClass.class).getBlsSetting();
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    return null;
  }

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
