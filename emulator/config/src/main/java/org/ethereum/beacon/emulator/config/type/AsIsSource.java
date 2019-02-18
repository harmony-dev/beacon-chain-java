package org.ethereum.beacon.emulator.config.type;

import org.ethereum.beacon.emulator.config.data.Config;

/** Source of config from config without changes */
public class AsIsSource implements ConfigSource {
  final Type type = Type.ASIS;
  final Config config;

  public AsIsSource(Config config) {
    this.config = config;
  }

  @Override
  public Type getType() {
    return type;
  }

  public Config getConfig() {
    return config;
  }

  @Override
  public String toString() {
    return "AsIsSource{" + "config=" + config + '}';
  }
}
