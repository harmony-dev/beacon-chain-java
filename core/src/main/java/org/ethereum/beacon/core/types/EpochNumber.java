package org.ethereum.beacon.core.types;

import org.ethereum.beacon.core.types.SlotNumber.EpochLength;
import tech.pegasys.artemis.util.uint.UInt64;

public class EpochNumber extends UInt64 implements
    SafeComparable<EpochNumber>, TypeIterable<EpochNumber> {

  public static final EpochNumber ZERO = of(0);

  public static EpochNumber of(int epochNum) {
    return new EpochNumber(UInt64.valueOf(epochNum));
  }

  EpochNumber(UInt64 uint) {
    super(uint);
  }

  @Override
  public EpochNumber plus(long unsignedAddend) {
    return new EpochNumber(super.plus(unsignedAddend));
  }

  @Override
  public EpochNumber plus(UInt64 addend) {
    return new EpochNumber(super.plus(addend));
  }

  public SlotNumber mul(EpochLength epochLength) {
    return new SlotNumber(times(epochLength));
  }

  public EpochNumber modulo(EpochNumber divisor) {
    return new EpochNumber(super.modulo(divisor));
  }

  @Override
  public EpochNumber increment() {
    return new EpochNumber(super.increment());
  }

  @Override
  public EpochNumber decrement() {
    return new EpochNumber(super.decrement());
  }

  @Override
  public EpochNumber zeroElement() {
    return ZERO;
  }
}
