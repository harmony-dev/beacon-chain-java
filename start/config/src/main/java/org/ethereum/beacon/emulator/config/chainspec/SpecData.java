package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ethereum.beacon.emulator.config.Config;
import org.ethereum.beacon.emulator.config.YamlPrinter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpecData implements Config {
  @JsonDeserialize(as = SpecConstantsDataImpl.class)
  private SpecConstantsData specConstants;
  private SpecHelpersData specHelpersOptions;

  public SpecConstantsData getSpecConstants() {
    return specConstants;
  }

  public void setSpecConstants(
      SpecConstantsData specConstants) {
    this.specConstants = specConstants;
  }

  public SpecHelpersData getSpecHelpersOptions() {
    return specHelpersOptions;
  }

  public void setSpecHelpersOptions(
      SpecHelpersData specHelpersOptions) {
    this.specHelpersOptions = specHelpersOptions;
  }

  @Override
  public String toString() {
    return new YamlPrinter(this).getString();
  }
}
