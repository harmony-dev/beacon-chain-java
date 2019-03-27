package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ethereum.beacon.emulator.config.Config;
import org.ethereum.beacon.emulator.config.YamlPrinter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpecData implements Config {
  public static final SpecData NOT_DEFINED = new SpecData();

  private SpecConstantsData specConstants;
  private SpecHelpersData specHelpersOptions = new SpecHelpersData();

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

  @JsonIgnore
  public boolean isDefined() {
    return this != NOT_DEFINED;
  }

  @Override
  public String toString() {
    return new YamlPrinter(this).getString();
  }
}
