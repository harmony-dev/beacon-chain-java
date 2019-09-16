package org.ethereum.beacon.discovery;

import org.ethereum.beacon.ssz.access.SSZBasicAccessor;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.basic.UIntPrimitive;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.visitor.SSZReader;
import tech.pegasys.artemis.util.bytes.Bytes4;

import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SSZSerializable(basicAccessor = NodeStatus.NodeStatusAccessor.class)
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

  private NodeStatus(int code) {
    this.code = code;
  }

  public static NodeStatus fromNumber(int i) {
    return codeMap.get(i);
  }

  public byte byteCode() {
    return (byte) code;
  }

  public static class NodeStatusAccessor extends UIntPrimitive {
    @Override
    public Set<Class> getSupportedClasses() {
      return new HashSet<Class>() {{add(NodeStatus.class);}};
    }

    @Override
    public int getSize(SSZField field) {
      return 1;
    }

    @Override
    public void encode(Object value, SSZField field, OutputStream result) {
      NodeStatus nodeStatus = (NodeStatus) value;
      SSZField overrided = new SSZField(byte.class, field.getFieldAnnotation(), "uint", 8, field.getName(), field.getGetter());
      super.encode(nodeStatus.byteCode(), overrided, result);
    }

    @Override
    public Object decode(SSZField field, SSZReader reader) {
      SSZField overrided = new SSZField(byte.class, field.getFieldAnnotation(), "uint", 8, field.getName(), field.getGetter());
      int code = (int) super.decode(overrided, reader);
      return NodeStatus.fromNumber(code);
    }
  }
}
