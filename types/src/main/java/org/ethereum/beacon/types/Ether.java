package org.ethereum.beacon.types;

import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Ether value.
 *
 * <p>Uses {@link UInt64} as an underlying type.
 */
public class Ether {

  /** Number of GWei in 1 Eth. */
  private static final long GWEI_IN_ETH = 1_000_000_000;

  /** The value. */
  private final UInt64 value;

  private Ether(UInt64 value) {
    this.value = value;
  }

  public static Ether valueOf(UInt64 value) {
    return new Ether(value);
  }

  public static Ether valueOf(long unsignedValue) {
    return new Ether(UInt64.valueOf(unsignedValue));
  }

  public UInt64 toGWei() {
    return value.times(GWEI_IN_ETH);
  }
}
