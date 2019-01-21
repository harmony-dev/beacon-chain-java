package org.ethereum.beacon.validator;

import java.util.concurrent.ExecutionException;

/**
 * Interface of validator service.
 *
 * <p>Validator service is responsible for:
 *
 * <ul>
 *   <li>registration on PoW chain;
 *   <li>block proposing;
 *   <li>block attesting.
 * </ul>
 */
public interface ValidatorService {

  /** Starts the service. */
  void start();

  /** Stops the service. */
  void stop();
}
