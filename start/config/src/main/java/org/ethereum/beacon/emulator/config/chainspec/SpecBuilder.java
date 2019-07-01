package org.ethereum.beacon.emulator.config.chainspec;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.uint.UInt64;

public class SpecBuilder {

  private SpecData spec;

  public SpecBuilder withSpec(SpecData spec) {
    this.spec = spec;
    return this;
  }

  public BeaconChainSpec buildSpec() {
    assert spec != null;
    return buildSpec(spec.getSpecHelpersOptions(), spec.getSpecConstants());
  }

  public BeaconChainSpec buildSpec(
      SpecHelpersData specHelpersOptions, SpecConstantsData specConstantsData) {
    return buildSpec(specHelpersOptions, buildSpecConstants(specConstantsData));
  }

  public BeaconChainSpec buildSpec(
      SpecHelpersData specHelpersOptions, SpecConstants specConstants) {
    return new BeaconChainSpec.Builder()
        .withDefaultHashFunction()
        .withDefaultHasher(specConstants)
        .withConstants(specConstants)
        .withBlsVerify(specHelpersOptions.isBlsVerify())
        .withBlsVerifyProofOfPossession(specHelpersOptions.isBlsVerifyProofOfPossession())
        .withCache(spec.getSpecHelpersOptions().isEnableCache())
        .build();
  }

  public SpecConstants buildSpecConstants() {
    assert spec != null;
    return buildSpecConstants(spec.getSpecConstants());
  }

  public static SpecConstants buildSpecConstants(SpecConstantsData specConstants) {

    DepositContractParametersData depositContractParameters = specConstants
        .getDepositContractParameters();
    GweiValuesData gweiValues = specConstants.getGweiValues();
    RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients = specConstants
        .getRewardAndPenaltyQuotients();
    HonestValidatorParametersData honestValidatorParameters = specConstants
        .getHonestValidatorParameters();
    InitialValuesData initialValues = specConstants.getInitialValues();
    MaxOperationsPerBlockData maxOperationsPerBlock = specConstants.getMaxOperationsPerBlock();
    MiscParametersData miscParameters = specConstants.getMiscParameters();
    StateListLengthsData stateListLengths = specConstants.getStateListLengths();
    TimeParametersData timeParameters = specConstants.getTimeParameters();

    return new SpecConstants() {
      @Override
      public Address getDepositContractAddress() {
        return depositContractParameters.getDepositContractAddress();
      }

      @Override
      public Gwei getMinDepositAmount() {
        return gweiValues.getMinDepositAmount();
      }

      @Override
      public Gwei getMaxEffectiveBalance() {
        return gweiValues.getMaxEffectiveBalance();
      }

      @Override
      public Gwei getEffectiveBalanceIncrement() {
        return gweiValues.getEffectiveBalanceIncrement();
      }

      @Override
      public UInt64 getMinSlashingPenaltyQuotient() {
        return rewardAndPenaltyQuotients.getMinSlashingPenaltyQuotient();
      }

      @Override
      public long getEth1FollowDistance() {
        return honestValidatorParameters.getEth1FollowDistance();
      }

      @Override
      public SlotNumber getGenesisSlot() {
        return initialValues.getGenesisSlot();
      }

      @Override
      public Hash32 getZeroHash() {
        return initialValues.getZeroHash();
      }

      @Override
      public UInt64 getBlsWithdrawalPrefix() {
        return initialValues.getBlsWithdrawalPrefix();
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
      public UInt64 getMaxIndicesPerAttestation() {
        return miscParameters.getMaxIndicesPerAttestation();
      }

      @Override
      public UInt64 getMinPerEpochChurnLimit() {
        return miscParameters.getMinPerEpochChurnLimit();
      }

      @Override
      public UInt64 getChurnLimitQuotient() {
        return miscParameters.getChurnLimitQuotient();
      }

      @Override
      public UInt64 getBaseRewardFactor() {
        return rewardAndPenaltyQuotients.getBaseRewardFactor();
      }

      @Override
      public UInt64 getWhistleblowingRewardQuotient() {
        return rewardAndPenaltyQuotients.getWhistleblowingRewardQuotient();
      }

      @Override
      public UInt64 getProposerRewardQuotient() {
        return rewardAndPenaltyQuotients.getProposerRewardQuotient();
      }

      @Override
      public UInt64 getInactivityPenaltyQuotient() {
        return rewardAndPenaltyQuotients.getInactivityPenaltyQuotient();
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
      public EpochNumber getSlotsPerEth1VotingPeriod() {
        return timeParameters.getSlotsPerEth1VotingPeriod();
      }

      @Override
      public EpochNumber getMinValidatorWithdrawabilityDelay() {
        return timeParameters.getMinValidatorWithdrawabilityDelay();
      }

      @Override
      public EpochNumber getGenesisEpoch() {
        return getGenesisSlot().dividedBy(getSlotsPerEpoch());
      }

      @Override
      public int getMaxTransfers() {
        return maxOperationsPerBlock.getMaxTransfers();
      }

      @Override
      public int getShuffleRoundCount() {
        return miscParameters.getShuffleRoundCount();
      }

      @Override
      public EpochNumber getPersistentCommitteePeriod() {
        return timeParameters.getPersistentCommitteePeriod();
      }

      @Override
      public EpochNumber getMaxEpochsPerCrosslink() {
        return timeParameters.getMaxEpochsPerCrosslink();
      }

      @Override
      public EpochNumber getMinEpochsToInactivityPenalty() {
        return timeParameters.getMinEpochsToInactivityPenalty();
      }

      @Override
      public SlotNumber getSlotsPerHistoricalRoot() {
        return timeParameters.getSlotsPerHistoricalRoot();
      }
    };
  }


}
