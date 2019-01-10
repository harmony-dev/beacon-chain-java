package org.ethereum.beacon.consensus.verifier;

import org.ethereum.beacon.core.BeaconBlock;

public interface BeaconBlockVerifier {

  VerificationResult validate(BeaconBlock block, Context context);

  class Context {}
}
