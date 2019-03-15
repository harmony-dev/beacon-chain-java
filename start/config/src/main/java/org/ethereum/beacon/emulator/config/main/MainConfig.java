package org.ethereum.beacon.emulator.config.main;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ethereum.beacon.emulator.config.Config;
import org.ethereum.beacon.emulator.config.chainspec.Spec;
import org.ethereum.beacon.emulator.config.main.plan.Plan;

/** Main application configuration */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MainConfig implements Config {
  private Plan plan;
  private Configuration config;
  private Spec chainSpec;

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

  public Spec getChainSpec() {
    return chainSpec;
  }

  public void setChainSpec(Spec chainSpec) {
    this.chainSpec = chainSpec;
  }
}
