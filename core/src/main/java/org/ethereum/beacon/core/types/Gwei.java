package org.ethereum.beacon.core.types;

import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable(serializeAs = UInt64.class)
public class Gwei extends UInt64 implements SafeComparable<Gwei> {
  private static final long GWEI_PER_ETHER = 1_000_000_000L;

  public static final Gwei ZERO = of(0);

  public Gwei(UInt64 uint) {
    super(uint);
  }

  public static Gwei ofEthers(int ethers) {
    return of(GWEI_PER_ETHER * ethers);
  }

  public static Gwei of(long gweis) {
    return new Gwei(UInt64.valueOf(gweis));
  }

  public static Gwei castFrom(UInt64 gweis) {
    return new Gwei(gweis);
  }

  public Gwei plus(Gwei addend) {
    return new Gwei(super.plus(addend));
  }

  public Gwei minus(Gwei subtrahend) {
    return new Gwei(super.minus(subtrahend));
  }

  /**
   * Saturation subtraction.
   *
   * @param subtrahend A Gwei representing an unsigned long to subtract.
   * @return {@link #MIN_VALUE} if underflowed, otherwise, result of {@link #minus(UInt64)}.
   */
  public Gwei minusSat(Gwei subtrahend) {
    if (this.compareTo(subtrahend) < 0) {
      return Gwei.castFrom(MIN_VALUE);
    } else {
      return minus(subtrahend);
    }
  }

  /**
   * Saturation addition.
   *
   * @param addend A Gwei representing an unsigned long to add.
   * @return {@link #MAX_VALUE} if overflowed, otherwise, result of {@link #plus(UInt64)}.
   */
  public Gwei plusSat(Gwei addend) {
    Gwei res = this.plus(addend);
    if (res.compareTo(this) <= 0 && addend.compareTo(Gwei.ZERO) > 0) {
      return Gwei.castFrom(MAX_VALUE);
    } else {
      return res;
    }
  }

  @Override
  public Gwei times(UInt64 unsignedMultiplier) {
    return new Gwei(super.times(unsignedMultiplier));
  }

  @Override
  public Gwei dividedBy(UInt64 divisor) {
    return new Gwei(super.dividedBy(divisor));
  }

  @Override
  public Gwei dividedBy(long divisor) {
    return new Gwei(super.dividedBy(divisor));
  }

  public Gwei mulDiv(Gwei multiplier, Gwei divisor) {
    return new Gwei(this.times(multiplier).dividedBy(divisor));
  }

  public Gwei times(int times) {
    return new Gwei(super.times(times));
  }

  @Override
  public String toString() {
    return getValue() % GWEI_PER_ETHER == 0 ? (getValue() / GWEI_PER_ETHER) + " Eth" :
        (getValue() / GWEI_PER_ETHER) + "." + (getValue() % GWEI_PER_ETHER) + " Eth";
  }
}
