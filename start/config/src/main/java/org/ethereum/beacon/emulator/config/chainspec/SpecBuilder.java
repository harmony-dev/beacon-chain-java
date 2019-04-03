package org.ethereum.beacon.emulator.config.chainspec;

import java.util.List;
import org.apache.milagro.amcl.BLS381.ECP;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.util.CachingBeaconChainSpec;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
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

    BeaconChainSpec defaultSpec = BeaconChainSpec.createWithSSZHasher(specConstants);
    return new CachingBeaconChainSpec(
        defaultSpec.getConstants(),
        defaultSpec.getHashFunction(),
        defaultSpec.getObjectHasher()) {

      @Override
      public PublicKey bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes) {
        if (specHelpersOptions.isBlsSign()) {
          return super.bls_aggregate_pubkeys(publicKeysBytes);
        } else {
          return PublicKey.create(new ECP());
        }
      }

      @Override
      public boolean bls_verify(
          BLSPubkey publicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
        if (specHelpersOptions.isBlsVerify()) {
          return super.bls_verify(publicKey, message, signature, domain);
        } else {
          return true;
        }
      }

      @Override
      public boolean bls_verify(
          PublicKey blsPublicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
        if (specHelpersOptions.isBlsVerify()) {
          return super.bls_verify(blsPublicKey, message, signature, domain);
        } else {
          return true;
        }
      }

      @Override
      public boolean bls_verify_multiple(
          List<PublicKey> publicKeys,
          List<Hash32> messages,
          BLSSignature signature,
          UInt64 domain) {
        if (specHelpersOptions.isBlsVerify()) {
          return super.bls_verify_multiple(publicKeys, messages, signature, domain);
        } else {
          return true;
        }
      }

      @Override
      public void process_deposit(MutableBeaconState state, Deposit deposit) {
        super.process_deposit_inner(state, deposit, specHelpersOptions.isBlsVerifyProofOfPossession());
      }
    };
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
      public EpochNumber getEpochsPerEth1VotingPeriod() {
        return timeParameters.getEpochsPerEth1VotingPeriod();
      }

      @Override
      public EpochNumber getMinValidatorWithdrawabilityDelay() {
        return timeParameters.getMinValidatorWithdrawabilityDelay();
      }

      @Override
      public SlotNumber getSlotsPerHistoricalRoot() {
        return timeParameters.getSlotsPerHistoricalRoot();
      }


    };
  }


}
