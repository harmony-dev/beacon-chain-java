package org.ethereum.beacon.core.types;

import tech.pegasys.artemis.util.uint.UInt64;

public class ValidatorIndex extends UInt64 implements
    SafeComparable<ValidatorIndex>, TypeIterable<ValidatorIndex> {

  public static final ValidatorIndex MAX = new ValidatorIndex(UInt64.MAX_VALUE);
  public static final ValidatorIndex ZERO = new ValidatorIndex(UInt64.ZERO);

  public static ValidatorIndex of(int index) {
    return new ValidatorIndex(UInt64.valueOf(index));
  }

  private ValidatorIndex(UInt64 uint) {
    super(uint);
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

  @Override
  public ValidatorIndex zeroElement() {
    return ZERO;
  }
}
