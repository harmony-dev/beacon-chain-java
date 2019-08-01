package org.ethereum.beacon.db.source.impl;

import java.util.function.Function;
import tech.pegasys.artemis.util.bytes.ArrayWrappingBytesValue;
import tech.pegasys.artemis.util.bytes.BytesValue;

public abstract class MemSizeEvaluators {
  private MemSizeEvaluators() {}

  public static final Function<BytesValue, Long> BytesValueEvaluator =
      value -> {
        if (value instanceof ArrayWrappingBytesValue) {
          // number of bytes + BytesValue header + array header + int offset + int length
          return (long) value.size() + 16 + 32 + 4 + 4;
        } else {
          return (long) value.size() + 16; // number of bytes + BytesValue header
        }
      };
}
