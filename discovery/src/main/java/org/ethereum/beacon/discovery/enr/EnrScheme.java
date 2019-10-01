package org.ethereum.beacon.discovery.enr;

import java.util.HashMap;
import java.util.Map;

/** Available identity schemes of {@link NodeRecord} */
public enum EnrScheme {
  V4("v4");

  private static final Map<String, EnrScheme> nameMap = new HashMap<>();

  static {
    for (EnrScheme scheme : EnrScheme.values()) {
      nameMap.put(scheme.name, scheme);
    }
  }

  private String name;

  private EnrScheme(String name) {
    this.name = name;
  }

  public static EnrScheme fromString(String name) {
    return nameMap.get(name);
  }

  public String stringName() {
    return name;
  }
}
