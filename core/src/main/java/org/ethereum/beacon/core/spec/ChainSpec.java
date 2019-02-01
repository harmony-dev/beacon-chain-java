package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.EpochNumber;

public interface ChainSpec
    extends InitialValues,
        MiscParameters,
        DepositContractParameters,
        TimeParameters,
        RewardAndPenaltyQuotients,
        MaxOperationsPerBlock,
        HonestValidatorParameters {

  ChainSpec DEFAULT = new ChainSpec(){};

  @Override
  default EpochNumber getGenesisEpoch() {
    return getGenesisSlot().dividedBy(getEpochLength());
  }
}
