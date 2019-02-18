package org.ethereum.beacon.emulator.config.type;

/** Source of config {@link org.ethereum.beacon.emulator.config.data.Config} */
public interface ConfigSource {
  Type getType();

  public enum Type {
    ASIS,
    YAML
  }
}
