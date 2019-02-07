package org.ethereum.beacon.core.types;

/** Various utility methods to work with {@link Time}. */
public abstract class Times {
  private Times() {}

  public static Millis currentTimeMillis() {
    return Millis.of(System.currentTimeMillis());
  }
}
