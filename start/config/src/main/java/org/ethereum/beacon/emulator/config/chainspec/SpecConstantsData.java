package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.emulator.config.Config;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.uint.UInt64;

/** SpecConstants settings object, creates {@link SpecConstants} from user data */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpecConstantsData implements Config {
  private DepositContractParametersData depositContractParameters;
  private HonestValidatorParametersData honestValidatorParameters;
  private InitialValuesData initialValues;
  private MaxOperationsPerBlockData maxOperationsPerBlock;
  private MiscParametersData miscParameters;
  private GweiValuesData gweiValues;
  private RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients;
  private StateListLengthsData stateListLengths;
  private TimeParametersData timeParameters;

  public SpecConstants build() {
    return new SpecConstants() {
      @Override
      public Address getDepositContractAddress() {
        return depositContractParameters.getDepositContractAddress();
      }

      @Override
      public UInt64 getDepositContractTreeDepth() {
        return depositContractParameters.getDepositContractTreeDepth();
      }

      @Override
      public Gwei getMinDepositAmount() {
        return gweiValues.getMinDepositAmount();
      }

      @Override
      public Gwei getMaxDepositAmount() {
        return gweiValues.getMaxDepositAmount();
      }

      @Override
      public Gwei getForkChoiceBalanceIncrement() {
        return gweiValues.getForkChoiceBalanceIncrement();
      }

      @Override
      public UInt64 getMinPenaltyQuotient() {
        return rewardAndPenaltyQuotients.getMinPenaltyQuotient();
      }

      @Override
      public long getEth1FollowDistance() {
        return honestValidatorParameters.getEth1FollowDistance();
      }

      @Override
      public UInt64 getGenesisForkVersion() {
        return initialValues.getGenesisForkVersion();
      }

      @Override
      public SlotNumber getGenesisSlot() {
        return initialValues.getGenesisSlot();
      }

      @Override
      public ShardNumber getGenesisStartShard() {
        return initialValues.getGenesisStartShard();
      }

      @Override
      public EpochNumber getFarFutureEpoch() {
        return initialValues.getFarFutureEpoch();
      }

      @Override
      public Hash32 getZeroHash() {
        return initialValues.getZeroHash();
      }

      @Override
      public BLSSignature getEmptySignature() {
        return initialValues.getEmptySignature();
      }

      @Override
      public Bytes1 getBlsWithdrawalPrefixByte() {
        return initialValues.getBlsWithdrawalPrefixByte();
      }

      @Override
      public int getMaxProposerSlashings() {
        return maxOperationsPerBlock.getMaxProposerSlashings();
      }

      @Override
      public int getMaxAttesterSlashings() {
        return maxOperationsPerBlock.getMaxAttesterSlashings();
      }

      @Override
      public int getMaxAttestations() {
        return maxOperationsPerBlock.getMaxAttestations();
      }

      @Override
      public int getMaxDeposits() {
        return maxOperationsPerBlock.getMaxDeposits();
      }

      @Override
      public int getMaxVoluntaryExits() {
        return maxOperationsPerBlock.getMaxVoluntaryExits();
      }

      @Override
      public ShardNumber getShardCount() {
        return miscParameters.getShardCount();
      }

      @Override
      public ValidatorIndex getTargetCommitteeSize() {
        return miscParameters.getTargetCommitteeSize();
      }

      @Override
      public Gwei getEjectionBalance() {
        return gweiValues.getEjectionBalance();
      }

      @Override
      public UInt64 getMaxBalanceChurnQuotient() {
        return miscParameters.getMaxBalanceChurnQuotient();
      }

      @Override
      public ShardNumber getBeaconChainShardNumber() {
        return miscParameters.getBeaconChainShardNumber();
      }

      @Override
      public UInt64 getMaxIndicesPerSlashableVote() {
        return miscParameters.getMaxIndicesPerSlashableVote();
      }

      @Override
      public UInt64 getMaxExitDequesPerEpoch() {
        return miscParameters.getMaxExitDequesPerEpoch();
      }

      @Override
      public UInt64 getBaseRewardQuotient() {
        return rewardAndPenaltyQuotients.getBaseRewardQuotient();
      }

      @Override
      public UInt64 getWhistleblowerRewardQuotient() {
        return rewardAndPenaltyQuotients.getWhistleblowerRewardQuotient();
      }

      @Override
      public UInt64 getAttestationInclusionRewardQuotient() {
        return rewardAndPenaltyQuotients.getAttestationInclusionRewardQuotient();
      }

      @Override
      public UInt64 getInactivityPenaltyQuotient() {
        return rewardAndPenaltyQuotients.getInactivityPenaltyQuotient();
      }

      @Override
      public SlotNumber getLatestBlockRootsLength() {
        return stateListLengths.getLatestBlockRootsLength();
      }

      @Override
      public EpochNumber getLatestRandaoMixesLength() {
        return stateListLengths.getLatestRandaoMixesLength();
      }

      @Override
      public EpochNumber getLatestActiveIndexRootsLength() {
        return stateListLengths.getLatestActiveIndexRootsLength();
      }

      @Override
      public EpochNumber getLatestSlashedExitLength() {
        return stateListLengths.getLatestSlashedExitLength();
      }

      @Override
      public Time getSecondsPerSlot() {
        return timeParameters.getSecondsPerSlot();
      }

      @Override
      public SlotNumber getMinAttestationInclusionDelay() {
        return timeParameters.getMinAttestationInclusionDelay();
      }

      @Override
      public SlotNumber.EpochLength getSlotsPerEpoch() {
        return timeParameters.getSlotsPerEpoch();
      }

      @Override
      public EpochNumber getMinSeedLookahead() {
        return timeParameters.getMinSeedLookahead();
      }

      @Override
      public EpochNumber getActivationExitDelay() {
        return timeParameters.getActivationExitDelay();
      }

      @Override
      public EpochNumber getEth1DataVotingPeriod() {
        return timeParameters.getEth1DataVotingPeriod();
      }

      @Override
      public EpochNumber getMinValidatorWithdrawabilityDelay() {
        return timeParameters.getMinValidatorWithdrawabilityDelay();
      }
    };
  }

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
