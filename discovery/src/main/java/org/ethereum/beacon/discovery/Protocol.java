package org.ethereum.beacon.discovery;

import java.util.HashMap;
import java.util.Map;

/** Discovery protocol versions */
public enum Protocol {
  V4("v4"),
  V5("v5");

  private static final Map<String, Protocol> nameMap = new HashMap<>();

  static {
    for (Protocol scheme : Protocol.values()) {
      nameMap.put(scheme.name, scheme);
    }
  }

  private String name;

  private Protocol(String name) {
    this.name = name;
  }

  public static Protocol fromString(String name) {
    return nameMap.get(name);
  }

  public String stringName() {
    return name;
  }
}
