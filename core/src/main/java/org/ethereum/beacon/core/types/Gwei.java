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

  public Gwei minusSat(Gwei subtrahend) {
    if (this.compareTo(subtrahend) < 0) {
      return Gwei.ZERO;
    } else {
      return minus(subtrahend);
    }
  }

  public Gwei plusSat(Gwei addend) {
    Gwei res = this.plus(addend);
    if (res.lessEqual(res) && addend.greater(Gwei.ZERO)) {
      return Gwei.castFrom(UInt64.MAX_VALUE);
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
        (getValue() / (double) GWEI_PER_ETHER) + " Eth";
  }
}
