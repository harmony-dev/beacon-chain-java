package org.ethereum.beacon.chain.pool.verifier;

import java.util.List;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;

public interface BatchVerifier {

  VerificationResult verify(List<ReceivedAttestation> batch);
}
