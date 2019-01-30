package org.ethereum.beacon.core.types;

import java.util.Iterator;
import tech.pegasys.artemis.util.uint.UInt64;

public class SlotNumber extends UInt64{

  public static final SlotNumber ZERO = of(0);

  public static class EpochLength extends SlotNumber {
    public EpochLength(UInt64 uint) {
      super(uint);
    }

    @Override
    public SlotNumber times(long unsignedMultiplier) {
      return new SlotNumber(super.times(unsignedMultiplier));
    }
  }

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

  public Iterable<SlotNumber> iterateFrom(SlotNumber toSlot) {
    return toSlot.iterateFrom(this);
  }

  public Iterable<SlotNumber> iterateTo(SlotNumber toSlot) {
    if (toSlot.less(this))
      throw new IllegalArgumentException("toSlot < this: " + toSlot + " < " + this);
    return () -> new SlotNumber.Iter(SlotNumber.this, toSlot, 1);
  }

  private static class Iter implements Iterator<SlotNumber> {
    int increment;
    SlotNumber cur;
    SlotNumber end;

    public Iter(SlotNumber from, SlotNumber to, int increment) {
      this.increment = increment;
      this.cur = from;
      this.end = to;
    }

    @Override
    public boolean hasNext() {
      return increment > 0 ? cur.compareTo(end) < 0 : cur.compareTo(end) > 0;
    }

    @Override
    public SlotNumber next() {
      SlotNumber ret = cur;
      cur = increment > 0 ? cur.plus(increment) : cur.minus(increment);
      return ret;
    }
  }
}
