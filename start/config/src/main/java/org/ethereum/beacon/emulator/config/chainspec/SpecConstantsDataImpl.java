package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ethereum.beacon.core.spec.SpecConstants;

/** SpecConstants settings object, creates {@link SpecConstants} from user data */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpecConstantsDataImpl implements SpecConstantsData {
  private DepositContractParametersData depositContractParameters;
  private HonestValidatorParametersData honestValidatorParameters;
  private InitialValuesData initialValues;
  private MaxOperationsPerBlockData maxOperationsPerBlock;
  private MiscParametersData miscParameters;
  private GweiValuesData gweiValues;
  private RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients;
  private StateListLengthsData stateListLengths;
  private TimeParametersData timeParameters;

  @Override
  public DepositContractParametersData getDepositContractParameters() {
    return depositContractParameters;
  }

  @Override
  public void setDepositContractParameters(
      DepositContractParametersData depositContractParameters) {
    this.depositContractParameters = depositContractParameters;
  }

  @Override
  public HonestValidatorParametersData getHonestValidatorParameters() {
    return honestValidatorParameters;
  }

  @Override
  public void setHonestValidatorParameters(
      HonestValidatorParametersData honestValidatorParameters) {
    this.honestValidatorParameters = honestValidatorParameters;
  }

  @Override
  public InitialValuesData getInitialValues() {
    return initialValues;
  }

  @Override
  public void setInitialValues(InitialValuesData initialValues) {
    this.initialValues = initialValues;
  }

  @Override
  public MaxOperationsPerBlockData getMaxOperationsPerBlock() {
    return maxOperationsPerBlock;
  }

  @Override
  public void setMaxOperationsPerBlock(MaxOperationsPerBlockData maxOperationsPerBlock) {
    this.maxOperationsPerBlock = maxOperationsPerBlock;
  }

  @Override
  public MiscParametersData getMiscParameters() {
    return miscParameters;
  }

  @Override
  public void setMiscParameters(MiscParametersData miscParameters) {
    this.miscParameters = miscParameters;
  }

  @Override
  public GweiValuesData getGweiValues() {
    return gweiValues;
  }

  @Override
  public void setGweiValues(GweiValuesData gweiValues) {
    this.gweiValues = gweiValues;
  }

  @Override
  public RewardAndPenaltyQuotientsData getRewardAndPenaltyQuotients() {
    return rewardAndPenaltyQuotients;
  }

  @Override
  public void setRewardAndPenaltyQuotients(
      RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients) {
    this.rewardAndPenaltyQuotients = rewardAndPenaltyQuotients;
  }

  @Override
  public StateListLengthsData getStateListLengths() {
    return stateListLengths;
  }

  @Override
  public void setStateListLengths(StateListLengthsData stateListLengths) {
    this.stateListLengths = stateListLengths;
  }

  @Override
  public TimeParametersData getTimeParameters() {
    return timeParameters;
  }

  @Override
  public void setTimeParameters(TimeParametersData timeParameters) {
    this.timeParameters = timeParameters;
  }
}
