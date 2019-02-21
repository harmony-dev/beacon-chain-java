package org.ethereum.beacon.emulator.config;

import org.ethereum.beacon.emulator.config.Config;

/** Uses some data to construct {@link Config} */
public interface ConfigSupplier {
  Config getConfig();
}
