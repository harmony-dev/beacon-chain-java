package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.EpochNumber;

public interface SpecConstants
    extends InitialValues,
        MiscParameters,
        StateListLengths,
        DepositContractParameters,
        TimeParameters,
        RewardAndPenaltyQuotients,
        MaxOperationsPerBlock,
        HonestValidatorParameters,
        GweiValues {

  SpecConstants DEFAULT = new SpecConstants() {};

  @Override
  default EpochNumber getGenesisEpoch() {
    return getGenesisSlot().dividedBy(getSlotsPerEpoch());
  }
}
