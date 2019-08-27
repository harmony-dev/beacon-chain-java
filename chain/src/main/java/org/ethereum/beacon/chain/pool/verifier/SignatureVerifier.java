package org.ethereum.beacon.chain.pool.verifier;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;

public interface SignatureVerifier {
  void feed(BeaconState state, IndexedAttestation indexed, ReceivedAttestation attestation);
  VerificationResult verify();
}
