package org.ethereum.beacon.start.common;

public abstract class ClientInfo {
  private static final String VERSION = ClientInfo.class.getPackage().getImplementationVersion();

  private ClientInfo() {}

  public static String version() {
    return VERSION;
  }

  public static String myTitle(Class myClass) {
    return myClass.getPackage().getImplementationTitle();
  }

  public static String fullTitleVersion(Class myClass) {
    return myTitle(myClass) + " v" + version();
  }
}
