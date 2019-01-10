package org.ethereum.beacon.core.state;

import java.util.stream.Stream;
import tech.pegasys.artemis.util.uint.UInt64;

/** Validator statuses. */
public enum ValidatorStatusFlag {
  EMPTY(Codes.EMPTY),
  INITIATED_EXIT(Codes.INITIATED_EXIT),
  WITHDRAWABLE(Codes.WITHDRAWABLE);

  /** Validator status codes. */
  public abstract static class Codes {
    private Codes() {}

    public static final UInt64 EMPTY = UInt64.valueOf(0);
    public static final UInt64 INITIATED_EXIT = UInt64.valueOf(1);
    public static final UInt64 WITHDRAWABLE = UInt64.valueOf(2);

    public static final UInt64 MAX_CODE = WITHDRAWABLE;
  }

  private UInt64 code;

  public static ValidatorStatusFlag valueOf(UInt64 code) {
    return Stream.of(values())
        .filter(status -> status.getCode() == code)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "Status code doesn't exist, got %s while MAX_CODE is %s",
                        code, Codes.MAX_CODE)));
  }

  ValidatorStatusFlag(UInt64 code) {
    this.code = code;
  }

  public UInt64 getCode() {
    return code;
  }
}
