package org.ethereum.beacon.core.types;

import java.util.function.Function;
import tech.pegasys.artemis.util.uint.UInt64;

public class ShardNumber extends UInt64 {

  public static final ShardNumber ZERO = of(0);

  public static ShardNumber of(int i) {
    return new ShardNumber(UInt64.valueOf(i));
  }

  public static ShardNumber of(UInt64 i) {
    return new ShardNumber(i);
  }

  public ShardNumber(UInt64 uint) {
    super(uint);
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
}
