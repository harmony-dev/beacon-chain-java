package org.ethereum.beacon.emulator.config.main;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ethereum.beacon.emulator.config.Config;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/** Main application configuration */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MainConfig implements Config {
  private Configuration config;
  private Plan plan;

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
}
