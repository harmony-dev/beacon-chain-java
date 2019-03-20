package org.ethereum.beacon.test.type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.ethereum.beacon.emulator.config.chainspec.DepositContractParametersData;
import org.ethereum.beacon.emulator.config.chainspec.GweiValuesData;
import org.ethereum.beacon.emulator.config.chainspec.HonestValidatorParametersData;
import org.ethereum.beacon.emulator.config.chainspec.InitialValuesData;
import org.ethereum.beacon.emulator.config.chainspec.MaxOperationsPerBlockData;
import org.ethereum.beacon.emulator.config.chainspec.MiscParametersData;
import org.ethereum.beacon.emulator.config.chainspec.RewardAndPenaltyQuotientsData;
import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsData;
import org.ethereum.beacon.emulator.config.chainspec.StateListLengthsData;
import org.ethereum.beacon.emulator.config.chainspec.TimeParametersData;

@JsonIgnoreProperties(ignoreUnknown = false)
public class SpecConstantsDataMerged implements SpecConstantsData {
  @JsonUnwrapped private DepositContractParametersData depositContractParameters;
  @JsonUnwrapped private HonestValidatorParametersData honestValidatorParameters;
  @JsonUnwrapped private InitialValuesData initialValues;
  @JsonUnwrapped private MaxOperationsPerBlockData maxOperationsPerBlock;
  @JsonUnwrapped private MiscParametersData miscParameters;
  @JsonUnwrapped private GweiValuesData gweiValues;
  @JsonUnwrapped private RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients;
  @JsonUnwrapped private StateListLengthsData stateListLengths;
  @JsonUnwrapped private TimeParametersData timeParameters;

  public DepositContractParametersData getDepositContractParameters() {
    return depositContractParameters;
  }

  public void setDepositContractParameters(
      DepositContractParametersData depositContractParameters) {
    this.depositContractParameters = depositContractParameters;
  }

  public HonestValidatorParametersData getHonestValidatorParameters() {
    return honestValidatorParameters;
  }

  public void setHonestValidatorParameters(
      HonestValidatorParametersData honestValidatorParameters) {
    this.honestValidatorParameters = honestValidatorParameters;
  }

  public InitialValuesData getInitialValues() {
    return initialValues;
  }

  public void setInitialValues(InitialValuesData initialValues) {
    this.initialValues = initialValues;
  }

  public MaxOperationsPerBlockData getMaxOperationsPerBlock() {
    return maxOperationsPerBlock;
  }

  public void setMaxOperationsPerBlock(MaxOperationsPerBlockData maxOperationsPerBlock) {
    this.maxOperationsPerBlock = maxOperationsPerBlock;
  }

  public MiscParametersData getMiscParameters() {
    return miscParameters;
  }

  public void setMiscParameters(MiscParametersData miscParameters) {
    this.miscParameters = miscParameters;
  }

  public GweiValuesData getGweiValues() {
    return gweiValues;
  }

  public void setGweiValues(GweiValuesData gweiValues) {
    this.gweiValues = gweiValues;
  }

  public RewardAndPenaltyQuotientsData getRewardAndPenaltyQuotients() {
    return rewardAndPenaltyQuotients;
  }

  public void setRewardAndPenaltyQuotients(
      RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients) {
    this.rewardAndPenaltyQuotients = rewardAndPenaltyQuotients;
  }

  public StateListLengthsData getStateListLengths() {
    return stateListLengths;
  }

  public void setStateListLengths(StateListLengthsData stateListLengths) {
    this.stateListLengths = stateListLengths;
  }

  public TimeParametersData getTimeParameters() {
    return timeParameters;
  }

  public void setTimeParameters(TimeParametersData timeParameters) {
    this.timeParameters = timeParameters;
  }
}
