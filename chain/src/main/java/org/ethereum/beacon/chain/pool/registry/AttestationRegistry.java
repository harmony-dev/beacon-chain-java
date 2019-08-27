package org.ethereum.beacon.chain.pool.registry;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;

public interface AttestationRegistry {
  boolean add(ReceivedAttestation attestation);
}
