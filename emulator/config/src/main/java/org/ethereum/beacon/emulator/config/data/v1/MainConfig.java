package org.ethereum.beacon.emulator.config.data.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ethereum.beacon.emulator.config.data.Config;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MainConfig implements Config {
  private final Integer version;
  private Configuration config;
  private Plan plan;

  public MainConfig() {
    this.version = 1;
  }

  public Configuration getConfig() {
    return config;
  }

  public void setConfig(Configuration config) {
    this.config = config;
  }

  public Plan getPlan() {
    return plan;
  }

  public void setPlan(Plan plan) {
    this.plan = plan;
  }

  @Override
  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    assert version == this.version;
  }
}
