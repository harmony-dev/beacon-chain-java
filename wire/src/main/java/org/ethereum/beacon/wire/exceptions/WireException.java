package org.ethereum.beacon.wire.exceptions;

public class WireException extends RuntimeException {

  public WireException(String message) {
    super(message);
  }

  public WireException(String message, Throwable cause) {
    super(message, cause);
  }

  public WireException(Throwable cause) {
    super(cause);
  }
}
