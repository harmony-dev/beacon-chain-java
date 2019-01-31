package org.ethereum.beacon.core.types;

import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable(serializeAs = UInt64.class)
public class Time extends UInt64 implements SafeComparable<Time> {

  public static final Time ZERO = of(0);

  public static Time of(long seconds) {
    return new Time(UInt64.valueOf(seconds));
  }

  public static Time castFrom(UInt64 time) {
    return new Time(time);
  }

  public Time(UInt64 uint) {
    super(uint);
  }

  public Time plus(Time addend) {
    return new Time(super.plus(addend));
  }

  public Time minus(Time subtrahend) {
    return new Time(super.minus(subtrahend));
  }

  @Override
  public Time times(UInt64 unsignedMultiplier) {
    return new Time(super.times(unsignedMultiplier));
  }

  public Time times(int times) {
    return new Time(super.times(times));
  }

  @Override
  public Time dividedBy(UInt64 divisor) {
    return new Time(super.dividedBy(divisor));
  }

  @Override
  public Time dividedBy(long divisor) {
    return new Time(super.dividedBy(divisor));
  }

}
