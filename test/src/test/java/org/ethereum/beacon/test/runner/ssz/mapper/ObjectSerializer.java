package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tech.pegasys.artemis.util.uint.UInt64;

import java.math.BigInteger;

public interface ObjectSerializer<V> {
  Class accepts();

  Object map(V instance);

  static void setUint64Field(ObjectNode node, String fieldName, UInt64 value) {
    node.set(fieldName, convert(value));
  }

  static JsonNode convert(UInt64 value) {
    return new ComparableBigIntegerNode(value.toBI());
  }

  public static class ComparableBigIntegerNode extends BigIntegerNode {
    ComparableBigIntegerNode(BigInteger v) {
      super(v);
    }

    @Override
    public boolean equals(Object o) {
      boolean superResult = super.equals(o);
      if (superResult) {
        return true;
      }

      if (o instanceof IntNode || o instanceof LongNode) {
        return ((NumericNode) o).bigIntegerValue().equals(this.bigIntegerValue());
      }

      return false;
    }
  }
}
