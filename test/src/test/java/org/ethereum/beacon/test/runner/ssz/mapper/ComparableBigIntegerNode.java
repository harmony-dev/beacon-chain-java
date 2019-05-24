package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import tech.pegasys.artemis.util.uint.UInt64;

import java.math.BigInteger;

/**
 * {@link BigIntegerNode} with overridden {@link #equals(Object)} method which ignores type of
 * {@link NumericNode} when comparing to another {@link NumericNode}, so only contents is compared.
 * Please note, as {@link #equals(Object)} method is called from the first object when comparing
 * like `a.equals(b)`, Object `a` should be of type {@link ComparableBigIntegerNode}
 */
public class ComparableBigIntegerNode extends BigIntegerNode {

  private ComparableBigIntegerNode(BigInteger v) {
    super(v);
  }

  static JsonNode valueOf(UInt64 value) {
    return new ComparableBigIntegerNode(value.toBI());
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
