package org.ethereum.beacon.pow.validator;

/** Fires by Eth1 service when it's not ready to process action, because sync is not complete */
public class IncompleteSyncException extends RuntimeException {
  public IncompleteSyncException(String message) {
    super(message);
  }
}
