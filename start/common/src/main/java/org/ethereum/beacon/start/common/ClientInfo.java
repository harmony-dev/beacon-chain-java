package org.ethereum.beacon.start.common;

public abstract class ClientInfo {
  private ClientInfo() {}

  private static final String VERSION = ClientInfo.class.getPackage().getImplementationVersion();

  public static String version() {
    return VERSION;
  }
}
