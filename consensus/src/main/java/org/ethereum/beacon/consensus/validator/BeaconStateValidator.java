package org.ethereum.beacon.consensus.validator;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

public interface BeaconStateValidator {

  ValidationResult validate(BeaconBlock block, BeaconState state);
}
