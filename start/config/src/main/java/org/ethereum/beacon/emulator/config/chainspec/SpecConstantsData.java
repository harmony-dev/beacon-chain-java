package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.emulator.config.Config;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.uint.UInt64;

/** SpecConstants settings object, creates {@link SpecConstants} from user data */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpecConstantsData {
  private DepositContractParametersData depositContractParameters;
  private HonestValidatorParametersData honestValidatorParameters;
  private InitialValuesData initialValues;
  private MaxOperationsPerBlockData maxOperationsPerBlock;
  private MiscParametersData miscParameters;
  private GweiValuesData gweiValues;
  private RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients;
  private StateListLengthsData stateListLengths;
  private TimeParametersData timeParameters;

  public DepositContractParametersData getDepositContractParameters() {
    return depositContractParameters;
  }

  public void setDepositContractParameters(DepositContractParametersData depositContractParameters) {
    this.depositContractParameters = depositContractParameters;
  }

  public HonestValidatorParametersData getHonestValidatorParameters() {
    return honestValidatorParameters;
  }

  public void setHonestValidatorParameters(HonestValidatorParametersData honestValidatorParameters) {
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

  public void setRewardAndPenaltyQuotients(RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients) {
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
