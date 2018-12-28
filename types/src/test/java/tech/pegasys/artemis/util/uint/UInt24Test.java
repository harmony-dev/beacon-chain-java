package tech.pegasys.artemis.util.uint;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class UInt24Test {

  @Test
  public void overflowInConstructor() {
    int maxValue = UInt24.MAX_VALUE.getValue();

    assertThat(UInt24.valueOf(maxValue + 1)).isEqualTo(UInt24.MIN_VALUE);
    assertThat(UInt24.valueOf(maxValue + 10)).isEqualTo(UInt24.MIN_VALUE.plus(9));
  }

  @Test
  public void underflowInConstructor() {
    int minValue = UInt24.MIN_VALUE.getValue();

    assertThat(UInt24.valueOf(minValue - 1)).isEqualTo(UInt24.MAX_VALUE);
    assertThat(UInt24.valueOf(minValue - 10)).isEqualTo(UInt24.MAX_VALUE.minus(9));
  }

  @Test
  public void overflowByAddition() {
    assertThat(UInt24.MAX_VALUE.plus(1)).isEqualTo(UInt24.MIN_VALUE);
    assertThat(UInt24.MAX_VALUE.plus(10)).isEqualTo(UInt24.MIN_VALUE.plus(9));
    assertThat(UInt24.MAX_VALUE.minus(5).plus(10)).isEqualTo(UInt24.MIN_VALUE.plus(4));
  }

  @Test
  public void underflowBySubtraction() {
    assertThat(UInt24.MIN_VALUE.minus(1)).isEqualTo(UInt24.MAX_VALUE);
    assertThat(UInt24.MIN_VALUE.minus(10)).isEqualTo(UInt24.MAX_VALUE.minus(9));
    assertThat(UInt24.MIN_VALUE.plus(5).minus(10)).isEqualTo(UInt24.MAX_VALUE.minus(4));
  }
}
