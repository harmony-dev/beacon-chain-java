package org.ethereum.beacon.emulator.config;

import org.ethereum.beacon.emulator.config.Config;

/** Passes config out without any modifications or AS IS */
public class AsIsSupplier implements ConfigSupplier {
  private final Config config;

  public AsIsSupplier(Config config) {
    this.config = config;
  }

  @Override
  public Config getConfig() {
    return config;
  }
}
