package org.ethereum.beacon.core.types;

import java.util.Iterator;
import tech.pegasys.artemis.util.uint.UInt64;

public class ValidatorIndex extends UInt64 implements SafeCompare<ValidatorIndex> {

  public static final ValidatorIndex MAX = new ValidatorIndex(UInt64.MAX_VALUE);
  public static final ValidatorIndex ZERO = new ValidatorIndex(UInt64.ZERO);

  public static ValidatorIndex of(int index) {
    return new ValidatorIndex(UInt64.valueOf(index));
  }

  ValidatorIndex(UInt64 uint) {
    super(uint);
  }

  public Iterable<ValidatorIndex> iterateFromZero() {
    return () -> new Iter(ZERO, ValidatorIndex.this, 1);
  }

  @Override
  public ValidatorIndex increment() {
    return new ValidatorIndex(super.increment());
  }

  @Override
  public ValidatorIndex decrement() {
    return new ValidatorIndex(super.decrement());
  }

  @Override
  public ValidatorIndex plus(long unsignedAddend) {
    return new ValidatorIndex(super.plus(unsignedAddend));
  }

  @Override
  public ValidatorIndex plus(UInt64 addend) {
    return new ValidatorIndex(super.plus(addend));
  }

  @Override
  public ValidatorIndex minus(long unsignedSubtrahend) {
    return new ValidatorIndex(super.minus(unsignedSubtrahend));
  }

  @Override
  public ValidatorIndex minus(UInt64 subtrahend) {
    return new ValidatorIndex(super.minus(subtrahend));
  }

  private static class Iter implements Iterator<ValidatorIndex> {
    int increment;
    ValidatorIndex cur;
    ValidatorIndex end;

    public Iter(ValidatorIndex from, ValidatorIndex to, int increment) {
      this.increment = increment;
      this.cur = from;
      this.end = to;
    }

    @Override
    public boolean hasNext() {
      return increment > 0 ? cur.less(end): cur.greater(end);
    }

    @Override
    public ValidatorIndex next() {
      ValidatorIndex ret = cur;
      cur = increment > 0 ? cur.plus(increment) : cur.minus(increment);
      return ret;
    }
  }
}
