package org.ethereum.beacon.emulator.config.data;

/**
 * Abstract configuration, any configuration to be used with {@link
 * org.ethereum.beacon.emulator.config.ConfigBuilder} should implement this interface
 */
public interface Config {
  int getVersion();
}
