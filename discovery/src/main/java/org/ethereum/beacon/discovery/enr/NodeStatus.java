package org.ethereum.beacon.discovery.enr;

import java.util.HashMap;
import java.util.Map;

public enum NodeStatus {
  ACTIVE(0x01),
  SLEEP(0x02),
  DEAD(0x03);

  private static final Map<Integer, NodeStatus> codeMap = new HashMap<>();

  static {
    for (NodeStatus type : NodeStatus.values()) {
      codeMap.put(type.code, type);
    }
  }

  private int code;

  NodeStatus(int code) {
    this.code = code;
  }

  public static NodeStatus fromNumber(int i) {
    return codeMap.get(i);
  }

  public byte byteCode() {
    return (byte) code;
  }
}
