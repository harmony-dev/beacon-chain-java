package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.SlotNumber.EpochLength;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public interface ChainSpec
    extends InitialValues,
        MiscParameters,
        DepositContractParameters,
        TimeParameters,
        RewardAndPenaltyQuotients,
        MaxOperationsPerBlock,
        HonestValidatorParameters {

  ChainSpec DEFAULT = new DefaultChainSpec();

  class DefaultChainSpec implements ChainSpec {
    @Override
    public ShardNumber getShardCount() {
      return SHARD_COUNT;
    }

    @Override
    public ValidatorIndex getTargetCommitteeSize() {
      return TARGET_COMMITTEE_SIZE;
    }

    @Override
    public Gwei getEjectionBalance() {
      return EJECTION_BALANCE;
    }

    @Override
    public UInt64 getMaxBalanceChurnQuotient() {
      return MAX_BALANCE_CHURN_QUOTIENT;
    }

    @Override
    public ShardNumber getBeaconChainShardNumber() {
      return BEACON_CHAIN_SHARD_NUMBER;
    }

    @Override
    public int getMaxCasperVotes() {
      return MAX_CASPER_VOTES;
    }

    @Override
    public SlotNumber getLatestBlockRootsLength() {
      return LATEST_BLOCK_ROOTS_LENGTH;
    }

    @Override
    public EpochNumber getLatestRandaoMixesLength() {
      return LATEST_RANDAO_MIXES_LENGTH;
    }

    @Override
    public EpochNumber getLatestPenalizedExitLength() {
      return LATEST_PENALIZED_EXIT_LENGTH;
    }

    @Override
    public UInt64 getMaxWithdrawalsPerEpoch() {
      return MAX_WITHDRAWALS_PER_EPOCH;
    }

    @Override
    public Address getDepositContractAddress() {
      return DEPOSIT_CONTRACT_ADDRESS;
    }

    @Override
    public UInt64 getDepositContractTreeDepth() {
      return DEPOSIT_CONTRACT_TREE_DEPTH;
    }

    @Override
    public Gwei getMinDeposit() {
      return MIN_DEPOSIT;
    }

    @Override
    public Gwei getMaxDeposit() {
      return MAX_DEPOSIT;
    }

    @Override
    public UInt64 getGenesisForkVersion() {
      return GENESIS_FORK_VERSION;
    }

    @Override
    public SlotNumber getGenesisSlot() {
      return GENESIS_SLOT;
    }

    @Override
    public ShardNumber getGenesisStartShard() {
      return GENESIS_START_SHARD;
    }

    @Override
    public SlotNumber getFarFutureSlot() {
      return FAR_FUTURE_SLOT;
    }

    @Override
    public BLSSignature getEmptySignature() {
      return EMPTY_SIGNATURE;
    }

    @Override
    public Bytes1 getBlsWithdrawalPrefixByte() {
      return BLS_WITHDRAWAL_PREFIX_BYTE;
    }

    @Override
    public UInt64 getSlotDuration() {
      return SLOT_DURATION;
    }

    @Override
    public SlotNumber getMinAttestationInclusionDelay() {
      return MIN_ATTESTATION_INCLUSION_DELAY;
    }

    @Override
    public EpochLength getEpochLength() {
      return EPOCH_LENGTH;
    }

    @Override
    public SlotNumber getSeedLookahead() {
      return SEED_LOOKAHEAD;
    }

    @Override
    public SlotNumber getEntryExitDelay() {
      return ENTRY_EXIT_DELAY;
    }

    @Override
    public SlotNumber getEth1DataVotingPeriod() {
      return ETH1_DATA_VOTING_PERIOD;
    }

    @Override
    public SlotNumber getMinValidatorWithdrawalTime() {
      return MIN_VALIDATOR_WITHDRAWAL_TIME;
    }

    @Override
    public UInt64 getBaseRewardQuotient() {
      return BASE_REWARD_QUOTIENT;
    }

    @Override
    public UInt64 getWhistleblowerRewardQuotient() {
      return WHISTLEBLOWER_REWARD_QUOTIENT;
    }

    @Override
    public UInt64 getIncluderRewardQuotient() {
      return INCLUDER_REWARD_QUOTIENT;
    }

    @Override
    public UInt64 getInactivityPenaltyQuotient() {
      return INACTIVITY_PENALTY_QUOTIENT;
    }

    @Override
    public int getMaxProposerSlashings() {
      return MAX_PROPOSER_SLASHINGS;
    }

    @Override
    public int getMaxCasperSlashings() {
      return MAX_CASPER_SLASHINGS;
    }

    @Override
    public int getMaxAttestations() {
      return MAX_ATTESTATIONS;
    }

    @Override
    public int getMaxDeposits() {
      return MAX_DEPOSITS;
    }

    @Override
    public int getMaxExits() {
      return MAX_EXITS;
    }

    @Override
    public long getEth1FollowDistance() {
      return ETH1_FOLLOW_DISTANCE;
    }
  };
}
