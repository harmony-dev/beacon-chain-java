package org.ethereum.beacon.emulator.config.main;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Signer.Insecure.class, name = "insecure"),
})
public abstract class Signer {

  public static class Insecure extends Signer{
    List<ValidatorKeys> keys;

    public List<ValidatorKeys> getKeys() {
      return keys;
    }

    public void setKeys(List<ValidatorKeys> keys) {
      this.keys = keys;
    }
  }
}
