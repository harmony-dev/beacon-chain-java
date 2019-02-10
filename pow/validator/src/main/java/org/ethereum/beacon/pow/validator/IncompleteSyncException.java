package org.ethereum.beacon.pow.validator;

public class IncompleteSyncException extends RuntimeException {
  public IncompleteSyncException(String message) {
    super(message);
  }
}
