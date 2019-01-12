package org.ethereum.beacon.consensus.verifier;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

public interface BeaconStateVerifier {

  VerificationResult validate(BeaconBlock block, BeaconState state);
}
