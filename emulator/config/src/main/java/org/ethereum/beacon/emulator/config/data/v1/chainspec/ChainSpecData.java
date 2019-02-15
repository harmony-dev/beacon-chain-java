package org.ethereum.beacon.emulator.config.data.v1.chainspec;

import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.emulator.config.data.Config;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.uint.UInt64;

public class ChainSpecData implements Config {
  private Integer version;
  private DepositContractParametersData depositContractParameters;
  private HonestValidatorParametersData honestValidatorParameters;
  private InitialValuesData initialValues;
  private MaxOperationsPerBlockData maxOperationsPerBlock;
  private MiscParametersData miscParameters;
  private RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients;
  private StateListLengthsData stateListLengths;
  private TimeParametersData timeParameters;

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public ChainSpec build() {
    return new ChainSpec() {
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
        return depositContractParameters.getMinDepositAmount();
      }

      @Override
      public Gwei getMaxDepositAmount() {
        return depositContractParameters.getMaxDepositAmount();
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
      public int getMaxExits() {
        return maxOperationsPerBlock.getMaxExits();
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
        return miscParameters.getEjectionBalance();
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
      public UInt64 getMaxWithdrawalsPerEpoch() {
        return miscParameters.getMaxWithdrawalsPerEpoch();
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
      public UInt64 getIncluderRewardQuotient() {
        return rewardAndPenaltyQuotients.getIncluderRewardQuotient();
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
      public EpochNumber getLatestIndexRootsLength() {
        return stateListLengths.getLatestIndexRootsLength();
      }

      @Override
      public EpochNumber getLatestPenalizedExitLength() {
        return stateListLengths.getLatestPenalizedExitLength();
      }

      @Override
      public Time getSlotDuration() {
        return timeParameters.getSlotDuration();
      }

      @Override
      public SlotNumber getMinAttestationInclusionDelay() {
        return timeParameters.getMinAttestationInclusionDelay();
      }

      @Override
      public SlotNumber.EpochLength getEpochLength() {
        return timeParameters.getEpochLength();
      }

      @Override
      public EpochNumber getSeedLookahead() {
        return timeParameters.getSeedLookahead();
      }

      @Override
      public EpochNumber getEntryExitDelay() {
        return timeParameters.getEntryExitDelay();
      }

      @Override
      public EpochNumber getEth1DataVotingPeriod() {
        return timeParameters.getEth1DataVotingPeriod();
      }

      @Override
      public EpochNumber getMinValidatorWithdrawalEpochs() {
        return timeParameters.getMinValidatorWithdrawalEpochs();
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
