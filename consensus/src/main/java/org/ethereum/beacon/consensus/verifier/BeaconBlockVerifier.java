package org.ethereum.beacon.consensus.verifier;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

public interface BeaconBlockVerifier {

  VerificationResult verify(BeaconBlock block, BeaconState state);
}
