package org.ethereum.beacon.validator.api;

/* Indicates that request couldn't be fulfilled for this input due to service limitations */
public class NotAcceptableInputException extends RuntimeException {
  public NotAcceptableInputException() {}

  public NotAcceptableInputException(String message) {
    super(message);
  }

  public NotAcceptableInputException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotAcceptableInputException(Throwable cause) {
    super(cause);
  }
}
