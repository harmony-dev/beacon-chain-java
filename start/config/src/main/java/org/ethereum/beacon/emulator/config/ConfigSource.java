package org.ethereum.beacon.emulator.config;

/** Source of config {@link org.ethereum.beacon.emulator.config.Config} */
public interface ConfigSource {
  Type getType();

  public enum Type {
    ASIS,
    YAML
  }
}
