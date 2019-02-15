package org.ethereum.beacon.emulator.config;

import org.ethereum.beacon.emulator.config.version.Config;

/** Uses object instance to construct {@link Config} */
public interface ConfigReader {
  Config readConfig(Object obj);
}
