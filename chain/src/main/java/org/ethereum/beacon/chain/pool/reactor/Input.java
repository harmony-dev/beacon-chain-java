package org.ethereum.beacon.chain.pool.reactor;

/**
 * Facilitate an ability heterogeneous
 *
 */
public class Input {

  /**
   * Wrapped value
   */
  private final Object value;

  private Input(Object value) {
    this.value = value;
  }

  public static Input wrap(Object value) {
    return new Input(value);
  }

  public Class getType() {
    return value.getClass();
  }

  @SuppressWarnings("unchecked")
  <T> T unbox() {
    return (T) value;
  }
}
