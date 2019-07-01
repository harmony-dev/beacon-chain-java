package org.ethereum.beacon.core.spec;

public interface ChainStartParameters {
  int GENESIS_ACTIVE_VALIDATOR_COUNT = 1 << 16;

  default int getGenesisActiveValidatorCount() {
    return GENESIS_ACTIVE_VALIDATOR_COUNT;
  }
}
