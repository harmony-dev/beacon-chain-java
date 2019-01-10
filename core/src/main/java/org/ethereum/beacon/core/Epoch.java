package org.ethereum.beacon.core;

/** Various epoch constants. */
public abstract class Epoch {
  private Epoch() {}

  /** Number of slots in the epoch. */
  public static final int LENGTH = 1 << 6; // 64
}
