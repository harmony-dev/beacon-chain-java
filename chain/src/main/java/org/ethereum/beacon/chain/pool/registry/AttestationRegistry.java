package org.ethereum.beacon.chain.pool.registry;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;

/**
 * An attestation registry interface.
 *
 * <p>Usually, an implementation of this interface tracks a set of attestation or identities of
 * attestations passed on it.
 */
public interface AttestationRegistry {

  /**
   * Adds attestation to the registry.
   *
   * @param attestation an attestation to be registered.
   * @return {@link true} if an attestation is new to the registry, {@link false} if the attestation
   *     has been already added.
   */
  boolean add(ReceivedAttestation attestation);
}
