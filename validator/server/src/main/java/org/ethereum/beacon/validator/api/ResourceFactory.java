package org.ethereum.beacon.validator.api;

import org.ethereum.beacon.validator.api.model.Genesis_time;
import org.ethereum.beacon.validator.api.model.Version;

public class ResourceFactory {
  private final static Version version = new Version();
  private final static Genesis_time genesis_time = new Genesis_time();

  public static Version getVersion( ) {
    return version;
  }

  public static Genesis_time getGenesis_time() {
    return genesis_time;
  }
}
