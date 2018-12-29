package org.ethereum.beacon.core;

import tech.pegasys.artemis.util.uint.UInt64;

/** Various slot constants. */
public abstract class Slot {
  private Slot() {}

  /** Slot number of initial block (genesis). */
  public static final UInt64 INITIAL_NUMBER = UInt64.ZERO;
}
