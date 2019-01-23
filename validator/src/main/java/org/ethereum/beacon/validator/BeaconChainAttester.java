package org.ethereum.beacon.validator;

import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;

public interface BeaconChainAttester {

  Attestation attest(ObservableBeaconState state);
}
