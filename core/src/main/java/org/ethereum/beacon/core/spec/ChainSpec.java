package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.EpochNumber;

public interface ChainSpec
    extends InitialValues,
        MiscParameters,
        StateListLengths,
        DepositContractParameters,
        TimeParameters,
        RewardAndPenaltyQuotients,
        MaxOperationsPerBlock,
        HonestValidatorParameters,
        GweiValues {

  ChainSpec DEFAULT = new ChainSpec() {};

  @Override
  default EpochNumber getGenesisEpoch() {
    return getGenesisSlot().dividedBy(getSlotsPerEpoch());
  }
}
