package org.ethereum.beacon.validator;

import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.pending.ObservableBeaconState;

public interface BeaconBlockAttester {

  Attestation attest(ObservableBeaconState state);
}
