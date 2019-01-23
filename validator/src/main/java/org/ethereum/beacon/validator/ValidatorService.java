package org.ethereum.beacon.validator;

/** Interface of validator service. */
public interface ValidatorService {

  /** Starts the service. */
  void start();

  /** Stops the service. */
  void stop();
}
