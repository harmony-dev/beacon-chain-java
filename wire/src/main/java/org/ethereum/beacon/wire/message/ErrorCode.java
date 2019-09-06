package org.ethereum.beacon.wire.message;

public enum ErrorCode {
  OK(0),
  IvalidRequest(1),
  ServerError(2),
  Unknown(-1);

  private final int code;

  ErrorCode(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  public static ErrorCode fromCode(int code) {
    for (ErrorCode errorCode : values()) {
      if (errorCode.getCode() == code) return errorCode;
    }
    return Unknown;
  }
}
