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
}
