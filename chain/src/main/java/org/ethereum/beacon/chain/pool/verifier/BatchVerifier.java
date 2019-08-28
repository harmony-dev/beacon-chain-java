package org.ethereum.beacon.chain.pool.verifier;

import java.util.List;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.AttestationChecker;

/**
 * An interface of attestation verifier that processes attestations in a batch.
 *
 * <p>Opposed to {@link AttestationChecker}, verifier involves I/O operations and checks highly
 * demanded to CPU resources.
 *
 * <p>Verifying attestations in a batch aids resource saving by grouping them by beacon block root
 * and target they are attesting to. A state calculated for each of such groups is reused across the
 * group. It saves a lot of resources as state calculation is pretty heavy operation.
 *
 * <p>It is highly recommended to place this verifier to the end of verification pipeline due to its
 * high demand to computational resources.
 */
public interface BatchVerifier {

  /**
   * Verifies a batch of attestations.
   *
   * @param batch a batch.
   * @return result of verification.
   */
  VerificationResult verify(List<ReceivedAttestation> batch);
}
