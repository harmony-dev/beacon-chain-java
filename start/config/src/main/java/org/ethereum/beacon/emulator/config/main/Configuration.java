package org.ethereum.beacon.emulator.config.main;

/** Beacon chain configuration */
public class Configuration {
  private String db;
  private Validator validator;

  public String getDb() {
    return db;
  }

  public void setDb(String db) {
    this.db = db;
  }

  public Validator getValidator() {
    return validator;
  }

  public void setValidator(Validator validator) {
    this.validator = validator;
  }
}
