package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.types.Ether;
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
        MaxOperationsPerBlock {

  ChainSpec DEFAULT =
      new ChainSpec() {
        @Override
        public UInt64 getShardCount() {
          return SHARD_COUNT;
        }

        @Override
        public UInt24 getTargetCommitteeSize() {
          return TARGET_COMMITTEE_SIZE;
        }

        @Override
        public Ether getEjectionBalance() {
          return EJECTION_BALANCE;
        }

        @Override
        public UInt64 getMaxBalanceChurnQuotient() {
          return MAX_BALANCE_CHURN_QUOTIENT;
        }

        @Override
        public UInt64 getBeaconChainShardNumber() {
          return BEACON_CHAIN_SHARD_NUMBER;
        }

        @Override
        public int getMaxCasperVotes() {
          return MAX_CASPER_VOTES;
        }

        @Override
        public UInt64 getLatestBlockRootsLength() {
          return LATEST_BLOCK_ROOTS_LENGTH;
        }

        @Override
        public UInt64 getLatestRandaoMixesLength() {
          return LATEST_RANDAO_MIXES_LENGTH;
        }

        @Override
        public UInt64 getLatestPenalizedExitLength() {
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
        public Ether getMinDeposit() {
          return MIN_DEPOSIT;
        }

        @Override
        public Ether getMaxDeposit() {
          return MAX_DEPOSIT;
        }

        @Override
        public UInt64 getGenesisForkVersion() {
          return GENESIS_FORK_VERSION;
        }

        @Override
        public UInt64 getGenesisSlot() {
          return GENESIS_SLOT;
        }

        @Override
        public UInt64 getGenesisStartShard() {
          return GENESIS_START_SHARD;
        }

        @Override
        public UInt64 getFarFutureSlot() {
          return FAR_FUTURE_SLOT;
        }

        @Override
        public Bytes96 getEmptySignature() {
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
        public UInt64 getMinAttestationInclusionDelay() {
          return MIN_ATTESTATION_INCLUSION_DELAY;
        }

        @Override
        public UInt64 getEpochLength() {
          return EPOCH_LENGTH;
        }

        @Override
        public UInt64 getSeedLookahead() {
          return SEED_LOOKAHEAD;
        }

        @Override
        public UInt64 getEntryExitDelay() {
          return ENTRY_EXIT_DELAY;
        }

        @Override
        public UInt64 getEth1DataVotingPeriod() {
          return ETH1_DATA_VOTING_PERIOD;
        }

        @Override
        public UInt64 getMinValidatorWithdrawalTime() {
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
      };
}
