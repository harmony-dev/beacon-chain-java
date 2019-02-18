package org.ethereum.beacon.emulator.config;

import org.ethereum.beacon.emulator.config.data.Config;

/** Uses some data to construct {@link Config} */
public interface ConfigSupplier {
  Config getConfig();
}
