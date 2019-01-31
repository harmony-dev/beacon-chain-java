package org.ethereum.beacon.core.types;

import tech.pegasys.artemis.util.uint.UInt64;

public interface SafeComparable<C extends SafeComparable> extends Comparable<UInt64> {
  default boolean greater(C uint) {
    return compareTo((UInt64) uint) > 0;
  }

  default boolean less(C uint) {
    return compareTo((UInt64) uint) < 0;
  }

  default boolean greaterEqual(C uint) {
    return compareTo((UInt64) uint) >= 0;
  }

  default boolean lessEqual(C uint) {
    return compareTo((UInt64) uint) <= 0;
  }
}
