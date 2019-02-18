package org.ethereum.beacon.emulator.config.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Default implementation of {@link Config} designed for version extraction from config */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigVersion implements Config {
  private int version;

  @Override
  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
