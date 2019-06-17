package org.ethereum.beacon.validator.api;

public class ImportFailureException extends RuntimeException {
  public ImportFailureException() {
  }

  public ImportFailureException(String message) {
    super(message);
  }

  public ImportFailureException(String message, Throwable cause) {
    super(message, cause);
  }

  public ImportFailureException(Throwable cause) {
    super(cause);
  }
}
