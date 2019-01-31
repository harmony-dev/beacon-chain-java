package org.ethereum.beacon.core.types;

import tech.pegasys.artemis.util.uint.UInt64;

public class Gwei extends UInt64 implements SafeComparable<Gwei> {

  public static final Gwei ZERO = of(0);

  public static Gwei ofEthers(int ethers) {
    return of(1_000_000_000L * ethers);
  }

  public static Gwei of(long gweis) {
    return new Gwei(UInt64.valueOf(gweis));
  }

  public static Gwei castFrom(UInt64 gweis) {
    return new Gwei(gweis);
  }

  public Gwei(UInt64 uint) {
    super(uint);
  }

  public Gwei plus(Gwei addend) {
    return new Gwei(super.plus(addend));
  }

  public Gwei minus(Gwei subtrahend) {
    return new Gwei(super.minus(subtrahend));
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
}
