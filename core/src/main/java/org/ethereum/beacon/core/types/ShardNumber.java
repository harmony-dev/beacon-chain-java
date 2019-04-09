package org.ethereum.beacon.core.types;

import java.util.Arrays;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable(serializeAs = UInt64.class)
public class ShardNumber extends UInt64 implements
    SafeComparable<ShardNumber>, TypeIterable<ShardNumber> {

  public static final ShardNumber ZERO = of(0);

  public static ShardNumber of(int i) {
    return new ShardNumber(UInt64.valueOf(i));
  }

  public static ShardNumber of(long i) {
    return new ShardNumber(UInt64.valueOf(i));
  }

  public static ShardNumber of(UInt64 i) {
    return new ShardNumber(i);
  }

  public ShardNumber(UInt64 uint) {
    super(uint);
  }

  public ShardNumber(int i) {
    super(i);
  }

  public ShardNumber plusModulo(long addend, ShardNumber divisor) {
    return plusModulo(UInt64.valueOf(addend), divisor);
  }

  public ShardNumber plusModulo(UInt64 addend, ShardNumber divisor) {
    return new ShardNumber(this.plus(addend).modulo(divisor));
  }

  public ShardNumber safeModulo(Function<UInt64, UInt64> safeCalc, ShardNumber divisor) {
    return new ShardNumber(safeCalc.apply(this).modulo(divisor));
  }

  @Override
  public ShardNumber increment() {
    return new ShardNumber(super.increment());
  }

  @Override
  public ShardNumber decrement() {
    return new ShardNumber(super.decrement());
  }

  @Override
  public ShardNumber zeroElement() {
    return ZERO;
  }

  @Override
  public String toString() {
    return toString(null);
  }

  public String toString(@Nullable SpecConstants spec) {
    return spec == null ? super.toString() :
        spec.getBeaconChainShardNumber().equals(this) ? "Beacon" : super.toString();
  }
}