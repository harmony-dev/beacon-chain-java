package org.ethereum.beacon.validator.api;

public class InvalidRequestSyntaxException extends RuntimeException {
  public InvalidRequestSyntaxException() {
  }

  public InvalidRequestSyntaxException(String message) {
    super(message);
  }

  public InvalidRequestSyntaxException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidRequestSyntaxException(Throwable cause) {
    super(cause);
  }
}
