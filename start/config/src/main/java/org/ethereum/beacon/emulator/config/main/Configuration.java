package org.ethereum.beacon.emulator.config.main;

import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.emulator.config.main.network.Network;

/** Beacon chain configuration */
public class Configuration {
  private String name;
  private String db;
  private List<Network> networks = new ArrayList<>();
  private Validator validator;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDb() {
    return db;
  }

  public void setDb(String db) {
    this.db = db;
  }

  public List<Network> getNetworks() {
    return networks;
  }

  public void setNetworks(List<Network> networks) {
    this.networks = networks;
  }

  public Validator getValidator() {
    return validator;
  }

  public void setValidator(Validator validator) {
    this.validator = validator;
  }
}
