package org.ethereum.beacon.core.types;

import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable(serializeAs = UInt64.class)
public class SlotNumber extends UInt64 implements
    SafeComparable<SlotNumber>, TypeIterable<SlotNumber> {

  @SSZSerializable(serializeAs = UInt64.class)
  public static class EpochLength extends SlotNumber {
    public EpochLength(UInt64 uint) {
      super(uint);
    }

    @Override
    public SlotNumber times(long unsignedMultiplier) {
      return new SlotNumber(super.times(unsignedMultiplier));
    }
  }

  public static final SlotNumber ZERO = of(0);

  public static SlotNumber of(long slot) {
    return new SlotNumber(UInt64.valueOf(slot));
  }

  public static SlotNumber castFrom(UInt64 slot) {
    return new SlotNumber(slot);
  }

  public SlotNumber(UInt64 uint) {
    super(uint);
  }

  @Override
  public SlotNumber plus(long unsignedAddend) {
    return new SlotNumber(super.plus(unsignedAddend));
  }

  @Override
  public SlotNumber plus(UInt64 addend) {
    return new SlotNumber(super.plus(addend));
  }

  @Override
  public SlotNumber minus(long unsignedAddend) {
    return new SlotNumber(super.minus(unsignedAddend));
  }

  @Override
  public SlotNumber minus(UInt64 addend) {
    return new SlotNumber(super.minus(addend));
  }

  @Override
  public SlotNumber increment() {
    return new SlotNumber(super.increment());
  }

  @Override
  public SlotNumber decrement() {
    return new SlotNumber(super.decrement());
  }

  @Override
  public SlotNumber dividedBy(UInt64 divisor) {
    return new SlotNumber(super.dividedBy(divisor));
  }

  public EpochNumber dividedBy(EpochLength epochLength) {
    return new EpochNumber(super.dividedBy(epochLength));
  }

  public SlotNumber modulo(SlotNumber divisor) {
    return new SlotNumber(super.modulo(divisor));
  }

  @Override
  public SlotNumber zeroElement() {
    return ZERO;
  }
}
