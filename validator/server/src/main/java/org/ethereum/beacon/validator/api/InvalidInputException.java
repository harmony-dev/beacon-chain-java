package org.ethereum.beacon.validator.api;

/* Indicates incorrect set of input data: not enough data provided, bad type etc. */
public class InvalidInputException extends RuntimeException {
  public InvalidInputException() {}

  public InvalidInputException(String message) {
    super(message);
  }

  public InvalidInputException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidInputException(Throwable cause) {
    super(cause);
  }
}
