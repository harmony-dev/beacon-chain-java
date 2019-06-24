package org.ethereum.beacon.core.spec;

public interface ChainStartParameters {
  int GENESIS_ACTIVE_VALIDATOR_COUNT = 1 << 16;
  long SECONDS_PER_DAY = 86400;

  default int getGenesisActiveValidatorCount() {
    return GENESIS_ACTIVE_VALIDATOR_COUNT;
  }

  default long getSecondsPerDay() {
    return SECONDS_PER_DAY;
  }
}
