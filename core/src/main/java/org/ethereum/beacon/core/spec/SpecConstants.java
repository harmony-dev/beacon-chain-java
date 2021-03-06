package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.EpochNumber;
import tech.pegasys.artemis.util.uint.UInt64;

public interface SpecConstants
    extends NonConfigurableConstants,
        InitialValues,
        MiscParameters,
        StateListLengths,
        DepositContractParameters,
        TimeParameters,
        RewardAndPenaltyQuotients,
        MaxOperationsPerBlock,
        HonestValidatorParameters,
        GweiValues {

  @Override
  default EpochNumber getGenesisEpoch() {
    return getGenesisSlot().dividedBy(getSlotsPerEpoch());
  }

  /**
   * Used in list max size specification, search for string
   * spec.MAX_EPOCH_ATTESTATIONS
   */
  default UInt64 getMaxEpochAttestations() {
    return getSlotsPerEpoch().times(getMaxAttestations());
  }
}
