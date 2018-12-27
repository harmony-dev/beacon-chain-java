package org.ethereum.beacon.consensus.validator;

import org.ethereum.beacon.core.BeaconBlock;

public interface BeaconBlockValidator {

  ValidationResult validate(BeaconBlock block, Context context);

  class Context {}
}
