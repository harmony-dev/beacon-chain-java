package org.ethereum.beacon.discovery.enr;

import java.util.HashMap;
import java.util.Map;

/** Available identity schemas of Ethereum {@link NodeRecord} signature */
public enum IdentitySchema {
  V4("v4");

  private static final Map<String, IdentitySchema> nameMap = new HashMap<>();

  static {
    for (IdentitySchema scheme : IdentitySchema.values()) {
      nameMap.put(scheme.name, scheme);
    }
  }

  private String name;

  private IdentitySchema(String name) {
    this.name = name;
  }

  public static IdentitySchema fromString(String name) {
    return nameMap.get(name);
  }

  public String stringName() {
    return name;
  }
}
