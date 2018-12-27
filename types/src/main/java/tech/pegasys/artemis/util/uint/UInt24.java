package tech.pegasys.artemis.util.uint;

import java.util.Objects;

/** An immutable unsigned 24-bit precision integer. */
public class UInt24 implements Comparable<UInt24> {
  private static final int MODULO = (1 << 24);

  public static final UInt24 MAX_VALUE = valueOf(MODULO - 1);
  public static final UInt24 MIN_VALUE = valueOf(0);

  public static final UInt24 ZERO = MIN_VALUE;

  private final int value;

  private UInt24(int value) {
    this.value = value;
  }

  public UInt24(UInt24 uint) {
    this.value = uint.getValue();
  }

  public int getValue() {
    return value;
  }

  /**
   * Creates and returns a new instance of UInt24 representing the argument.
   *
   * @param unsignedValue An unsigned long.
   * @return A new UInt24 instance representing the given unsigned input.
   */
  public static UInt24 valueOf(int unsignedValue) {
    // handle overflows and underflows
    if (unsignedValue < 0) {
      int remainder = Math.abs(unsignedValue) % MODULO;
      return new UInt24(MODULO - remainder);
    } else if (unsignedValue >= MODULO) {
      return new UInt24(unsignedValue % MODULO);
    } else {
      return new UInt24(unsignedValue);
    }
  }

  /**
   * Creates and returns a new instance of UInt24 representing the argument. Parsing is done using
   * the {@link java.lang.Long#parseUnsignedLong(String) Long.parseUnsignedLong} method.
   *
   * @param unsignedStringValue A string representing an unsigned long (between 0 and 2^24-1).
   * @return A new UInt24 instance representing the given unsigned input.
   * @throws NumberFormatException If the argument cannot be parsed as an unsigned integer. (i.e. <0
   *     OR >2^24-1)
   */
  public static UInt24 valueOf(String unsignedStringValue) throws NumberFormatException {
    return new UInt24(Integer.parseUnsignedInt(unsignedStringValue));
  }

  /**
   * Increments the value by 1 and returns the result. Replicates the ++ operator.
   *
   * @return A new, incremented, UInt24.
   */
  public UInt24 increment() {
    return new UInt24(this.value + 1);
  }

  /**
   * Decrements the value by 1 and return the result. Replicates the -- operator.
   *
   * @return A new, decremented, UInt24.
   */
  public UInt24 decrement() {
    return new UInt24(this.value - 1);
  }

  /**
   * Adds the addend passed in the argument to specified object. The result is returned as a new
   * UInt24.
   *
   * @param unsignedAddend An unsigned long to add.
   * @return A new UInt24 containing the result of the addition operation.
   */
  public UInt24 plus(int unsignedAddend) {
    return new UInt24(this.value + unsignedAddend);
  }

  /**
   * Adds the addend passed in the argument to specified object. The result is returned as a new
   * UInt24.
   *
   * @param addend A UInt24 representing an unsigned long to add.
   * @return A new UInt24 containing the result of the addition operation.
   */
  public UInt24 plus(UInt24 addend) {
    return new UInt24(this.value + addend.getValue());
  }

  /**
   * Subtracts the subtrahend passed in the argument from the specified object. The result is
   * returned as a new UInt24.
   *
   * @param unsignedSubtrahend An unsigned long to subtract.
   * @return A new UInt24 containing the result of the subtraction operation.
   */
  public UInt24 minus(int unsignedSubtrahend) {
    return new UInt24(this.value - unsignedSubtrahend);
  }

  /**
   * Subtracts the subtrahend passed in the argument from the specified object. The result is
   * returned as a new UInt24.
   *
   * @param subtrahend A UInt24 representing an unsigned long to subtract.
   * @return A new UInt24 containing the result of the subtraction operation.
   */
  public UInt24 minus(UInt24 subtrahend) {
    return new UInt24(this.value - subtrahend.getValue());
  }

  @Override
  public int compareTo(UInt24 uint) {
    return Integer.compareUnsigned(this.value, uint.getValue());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof UInt24)) {
      return false;
    }

    UInt24 uint = (UInt24) o;

    return Integer.compareUnsigned(this.value, uint.getValue()) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return Integer.toUnsignedString(this.value);
  }
}
