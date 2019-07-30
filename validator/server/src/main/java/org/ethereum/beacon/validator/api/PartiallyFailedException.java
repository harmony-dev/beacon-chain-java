package org.ethereum.beacon.validator.api;

/* Indicates non-fatal failure */
public class PartiallyFailedException extends RuntimeException {
  public PartiallyFailedException() {}

  public PartiallyFailedException(String message) {
    super(message);
  }

  public PartiallyFailedException(String message, Throwable cause) {
    super(message, cause);
  }

  public PartiallyFailedException(Throwable cause) {
    super(cause);
  }
}
