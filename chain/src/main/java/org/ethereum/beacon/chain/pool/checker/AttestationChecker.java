package org.ethereum.beacon.chain.pool.checker;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;

/**
 * An interface of light weight attestation checker.
 *
 * <p>Implementations of this interface SHOULD NOT execute I/O operations or run checks that are in
 * high demand to CPU resources. Usually, implementation of this interface runs quick checks that
 * could be done with the attestation itself without involving any other data.
 */
public interface AttestationChecker {

  /**
   * Given attestation runs a check.
   *
   * @param attestation an attestation to check.
   * @return {@code true} if checks are passed successfully, {@code false} otherwise.
   */
  boolean check(ReceivedAttestation attestation);
}
