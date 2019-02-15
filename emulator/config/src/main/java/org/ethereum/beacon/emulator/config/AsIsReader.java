package org.ethereum.beacon.emulator.config;

import org.ethereum.beacon.emulator.config.version.Config;

/** Passes config out without any modifications or AS IS */
public class AsIsReader implements ConfigReader {

  @Override
  public Config readConfig(Object obj) {
    assert obj instanceof Config;
    return (Config) obj;
  }
}
