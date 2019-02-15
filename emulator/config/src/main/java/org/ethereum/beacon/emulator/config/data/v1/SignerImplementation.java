package org.ethereum.beacon.emulator.config.data.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class SignerImplementation {
  @JsonProperty("class")
  private String clazz;

  private Map<String, String> input;

  public String getClazz() {
    return clazz;
  }

  public void setClazz(String clazz) {
    this.clazz = clazz;
  }

  public Map<String, String> getInput() {
    return input;
  }

  public void setInput(Map<String, String> input) {
    this.input = input;
  }
}
