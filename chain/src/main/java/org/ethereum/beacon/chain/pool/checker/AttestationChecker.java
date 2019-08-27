package org.ethereum.beacon.chain.pool.checker;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;

public interface AttestationChecker {
  boolean check(ReceivedAttestation attestation);
}
