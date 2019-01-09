package org.ethereum.beacon.core.state;

import java.util.stream.Stream;
import tech.pegasys.artemis.util.uint.UInt64;

/** Validator statuses. */
public enum ValidatorStatus {
  EMPTY(),
  PENDING_ACTIVATION(Codes.PENDING_ACTIVATION),
  ACTIVE(Codes.ACTIVE),
  ACTIVE_PENDING_EXIT(Codes.ACTIVE_PENDING_EXIT),
  EXITED_WITHOUT_PENALTY(Codes.EXITED_WITHOUT_PENALTY),
  EXITED_WITH_PENALTY(Codes.EXITED_WITH_PENALTY);

  /** Validator status codes. */
  public abstract static class Codes {
    private Codes() {}

    public static final UInt64 PENDING_ACTIVATION = UInt64.valueOf(0);
    public static final UInt64 ACTIVE = UInt64.valueOf(1);
    public static final UInt64 ACTIVE_PENDING_EXIT = UInt64.valueOf(2);
    public static final UInt64 EXITED_WITHOUT_PENALTY = UInt64.valueOf(3);
    public static final UInt64 EXITED_WITH_PENALTY = UInt64.valueOf(4);

    public static final UInt64 MAX_CODE = EXITED_WITH_PENALTY;
  }

  private UInt64 code;

  public static ValidatorStatus valueOf(UInt64 code) {
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

  ValidatorStatus(UInt64 code) {
    this.code = code;
  }

  public UInt64 getCode() {
    return code;
  }
}
